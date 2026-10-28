package com.becommerce.crm.clinical.trichoscopy.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.clinical.trichoscopy.application.dto.TrichoscopyImageResponse;
import com.becommerce.crm.clinical.trichoscopy.application.port.out.TrichoscopyRepository;
import com.becommerce.crm.clinical.trichoscopy.domain.PatientTrichoscopyImage;
import com.becommerce.crm.clinical.trichoscopy.domain.exception.TrichoscopyImageNotFoundException;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyModulesRepository;
import com.becommerce.crm.masterdata.company.domain.CompanyModule;
import com.becommerce.crm.masterdata.company.domain.CompanyModules;
import com.becommerce.crm.masterdata.company.domain.CompanySegment;
import com.becommerce.crm.masterdata.storage.application.dto.StorageResponse;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrichoscopyServiceTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID STORAGE_ID = UUID.randomUUID();
    private static final byte[] JPEG = {1, 2, 3};

    @Mock
    private TrichoscopyRepository repository;

    @Mock
    private StorageUseCase storageUseCase;

    @Mock
    private CompanyModulesRepository companyModulesRepository;

    @Mock
    private TenantAuditRecorder auditor;

    private TrichoscopyService service;

    @BeforeEach
    void setUp() {
        service = new TrichoscopyService(repository, storageUseCase, companyModulesRepository, auditor);
    }

    private void segment(CompanySegment segment) {
        when(companyModulesRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(new CompanyModules(segment, Set.of(CompanyModule.PATIENTS))));
    }

    @Test
    void captureStoresTheImageAndLinksItToThePatient() {
        segment(CompanySegment.TRICHOLOGY);
        when(storageUseCase.uploadForContact(eq(COMPANY_ID), eq(CONTACT_ID), anyString(), eq("image/jpeg"),
                eq(JPEG), eq(USER_ID)))
                .thenReturn(new StorageResponse(STORAGE_ID, "key", "tricoscopia.jpg", "image/jpeg", 3,
                        COMPANY_ID, LocalDateTime.now(), CONTACT_ID));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TrichoscopyImageResponse response = service.capture(COMPANY_ID, CONTACT_ID, null, "image/jpeg", JPEG,
                "  couro cabeludo  ", USER_ID);

        assertThat(response.contactId()).isEqualTo(CONTACT_ID);
        assertThat(response.storageObjectId()).isEqualTo(STORAGE_ID);
        assertThat(response.notes()).isEqualTo("couro cabeludo");
        assertThat(response.downloadUrl())
                .isEqualTo("/api/v1/companies/" + COMPANY_ID + "/storage/" + STORAGE_ID + "?inline=true");
    }

    @Test
    void companyOutsideTrichologyIsDenied() {
        segment(CompanySegment.DENTAL);

        assertThatThrownBy(() -> service.list(COMPANY_ID, CONTACT_ID))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository, storageUseCase);
    }

    @Test
    void rejectsFilesThatAreNotImages() {
        segment(CompanySegment.TRICHOLOGY);

        assertThatThrownBy(() -> service.capture(COMPANY_ID, CONTACT_ID, "a.pdf", "application/pdf", JPEG,
                null, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPG, PNG ou WEBP");
        verifyNoInteractions(storageUseCase);
    }

    @Test
    void rejectsImagesLargerThanFiveMegabytes() {
        segment(CompanySegment.TRICHOLOGY);
        byte[] large = new byte[(int) TrichoscopyService.MAX_BYTES + 1];

        assertThatThrownBy(() -> service.capture(COMPANY_ID, CONTACT_ID, "a.jpg", "image/jpeg", large,
                null, USER_ID))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(storageUseCase);
    }

    @Test
    void listReturnsThePatientImages() {
        segment(CompanySegment.TRICHOLOGY);
        PatientTrichoscopyImage image = PatientTrichoscopyImage.create(COMPANY_ID, CONTACT_ID, STORAGE_ID,
                USER_ID, null);
        when(repository.findByCompanyIdAndContactId(COMPANY_ID, CONTACT_ID)).thenReturn(List.of(image));

        assertThat(service.list(COMPANY_ID, CONTACT_ID)).extracting(TrichoscopyImageResponse::id)
                .containsExactly(image.getId());
    }

    @Test
    void deleteRemovesTheRecordAndTheStoredFile() {
        segment(CompanySegment.TRICHOLOGY);
        PatientTrichoscopyImage image = PatientTrichoscopyImage.create(COMPANY_ID, CONTACT_ID, STORAGE_ID,
                USER_ID, null);
        when(repository.findByIdAndCompanyIdAndContactId(image.getId(), COMPANY_ID, CONTACT_ID))
                .thenReturn(Optional.of(image));

        service.delete(COMPANY_ID, CONTACT_ID, image.getId(), USER_ID);

        verify(repository).delete(image);
        verify(storageUseCase).delete(COMPANY_ID, STORAGE_ID);
    }

    @Test
    void deleteOfUnknownImageFailsWithoutTouchingStorage() {
        segment(CompanySegment.TRICHOLOGY);
        UUID imageId = UUID.randomUUID();
        when(repository.findByIdAndCompanyIdAndContactId(imageId, COMPANY_ID, CONTACT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(COMPANY_ID, CONTACT_ID, imageId, USER_ID))
                .isInstanceOf(TrichoscopyImageNotFoundException.class);
        verify(storageUseCase, never()).delete(any(), any());
    }
}
