package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    private SpringDataUserRepository repository;

    private UserRepositoryImpl userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepositoryImpl(repository, new UserMapper());
    }

    @Test
    void shouldGenerateIdWhenSavingNewUser() {
        User user = User.create(new Email("novo@crm.com"), new Password("Kc!Valid1Aa1"),
                "Novo", "Usuario", UUID.randomUUID());
        UUID generatedId = UUID.randomUUID();

        when(repository.existsById(any(UUID.class))).thenReturn(false);
        when(repository.save(any(UserJpaEntity.class))).thenAnswer(invocation -> {
            UserJpaEntity entity = invocation.getArgument(0);
            entity.setId(generatedId);
            return entity;
        });

        User saved = userRepository.save(user);

        assertEquals(generatedId, saved.getId());
        verify(repository).save(any(UserJpaEntity.class));
    }

    @Test
    void shouldPreserveIdWhenSavingExistingUser() {
        User user = User.create(new Email("existente@crm.com"), new Password("Kc!Valid1Aa1"),
                "Existente", "Usuario", UUID.randomUUID());

        when(repository.existsById(user.getId())).thenReturn(true);
        when(repository.save(any(UserJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userRepository.save(user);

        assertEquals(user.getId(), saved.getId());
        verify(repository).save(any(UserJpaEntity.class));
        verify(repository, never()).deleteById(any(UUID.class));
    }
}
