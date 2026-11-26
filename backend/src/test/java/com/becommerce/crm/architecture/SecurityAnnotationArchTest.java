package com.becommerce.crm.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AnalyzeClasses(packages = "com.becommerce.crm", importOptions = ImportOption.DoNotIncludeTests.class)
class SecurityAnnotationArchTest {

    private static final Pattern HAS_AUTHORITY = Pattern.compile("hasAuthority\\('([^']+)'\\)");
    private static final Pattern PREFIXO_MODULO = Pattern.compile("^([^:]+):");

    private static final DescribedPredicate<JavaMethod> ENDPOINT_HTTP = new DescribedPredicate<>("endpoint HTTP") {
        @Override
        public boolean test(JavaMethod method) {
            return method.tryGetAnnotationOfType(GetMapping.class).isPresent()
                    || method.tryGetAnnotationOfType(PostMapping.class).isPresent()
                    || method.tryGetAnnotationOfType(PutMapping.class).isPresent()
                    || method.tryGetAnnotationOfType(PatchMapping.class).isPresent()
                    || method.tryGetAnnotationOfType(DeleteMapping.class).isPresent();
        }
    };

    private static final Set<String> ENDPOINTS_SEM_PREAUTHORIZE = Set.of(
            // TODO revisar: webhook público do Asaas; autenticação por header asaas-access-token no método
            "AsaasWebhookController#receive",
            // TODO revisar: rota pública /api/v1/public/forms/open/{token}, validada por token e rate limit por IP
            "OpenFormController#preview",
            // TODO revisar: rota pública /api/v1/public/forms/open/{token}, validada por token e rate limit por IP
            "OpenFormController#submit",
            // TODO revisar: rota pública /api/v1/public/forms/{token}, validada por token e rate limit por IP
            "PatientFormRequestController#preview",
            // TODO revisar: rota pública /api/v1/public/forms/{token}, validada por token e rate limit por IP
            "PatientFormRequestController#submit",
            // TODO revisar: rota pública /public/forms/live/push/{publishToken}, protegida por token aleatório da sessão
            "LiveViewController#push",
            // TODO revisar: rota pública /public/forms/live/push/{publishToken}, protegida por token aleatório da sessão
            "LiveViewController#close",
            // TODO revisar: rota pública /public/forms/live/view/{viewToken}, protegida por token aleatório da sessão
            "LiveViewController#frame",
            // TODO revisar: handshake público da Meta, validado por hub.verify_token no método
            "WhatsAppWebhookController#verify",
            // TODO revisar: webhook validado por token e assinatura X-Hub-Signature-256 no método
            "WhatsAppWebhookController#handle",
            // webhook do Instagram: handshake por verify token e POST validado por X-Hub-Signature-256
            "InstagramWebhookController#verify",
            "InstagramWebhookController#handle",
            // TODO revisar: webhook validado por X-Telegram-Bot-Api-Secret-Token no método
            "TelegramWebhookController#receive",
            // TODO revisar: leitura exige login via @AuthenticationPrincipal; nenhuma authority de card fee definida
            "CardFeePresetController#listActive",
            // TODO revisar: exige isPlatformAdmin() manualmente; nenhuma authority de admin de taxa definida
            "CardFeePresetController#listAll",
            // TODO revisar: exige isPlatformAdmin() manualmente; nenhuma authority de admin de taxa definida
            "CardFeePresetController#create",
            // TODO revisar: exige isPlatformAdmin() manualmente; nenhuma authority de admin de taxa definida
            "CardFeePresetController#update",
            // TODO revisar: exige isPlatformAdmin() manualmente; nenhuma authority de admin de taxa definida
            "CardFeePresetController#deactivate",
            // TODO revisar: prévia pública do convite de empresa por token
            "CompanyInvitationController#preview",
            // TODO revisar: cadastro público a partir de convite de empresa por token
            "CompanyInvitationController#register",
            // TODO revisar: prévia pública do convite por token
            "InvitationController#preview",
            // TODO revisar: cadastro público a partir de convite por token
            "InvitationController#register",
            // TODO revisar: cadastro público de usuário, sem permissão de módulo aplicável
            "AuthController#register",
            // TODO revisar: recuperação de senha pública por e-mail
            "AuthController#forgotPassword",
            // TODO revisar: redefinição de senha pública por token
            "AuthController#resetPassword",
            // TODO revisar: troca de senha usa o principal autenticado, sem authority definida
            "AuthController#changePassword",
            // TODO revisar: dados da própria conta via principal autenticado, sem authority definida
            "AuthController#me",
            // TODO revisar: aceite de convite por token, sem login
            "UserController#acceptInvite",
            // TODO revisar: perfil do próprio usuário via principal autenticado, sem authority definida
            "UserController#getProfile",
            // TODO revisar: perfil do próprio usuário via principal autenticado, sem authority definida
            "UserController#updateProfile",
            // TODO revisar: API interna consumida por crm-auth-service; exige Jwt do Keycloak no método
            "IdentityInternalController#provision",
            // TODO revisar: API interna consumida por crm-auth-service; exige Jwt do Keycloak no método
            "IdentityInternalController#link",
            // TODO revisar: rota pública /api/v1/public/registration/{token}, validada por token e rate limit por IP
            "RegistrationLinkController#preview",
            // TODO revisar: rota pública /api/v1/public/registration/{token}, validada por token e rate limit por IP
            "RegistrationLinkController#submit",
            // rota pública /api/v1/public/trichology/quiz/{token}: token com hash na policy RLS e rate limit por IP
            "PublicTrichologyController#quiz",
            "PublicTrichologyController#submitQuiz",
            // verificação pública do relatório: função SECURITY DEFINER sem dados clínicos, rate limit por IP
            "PublicTrichologyController#verify"
    );

    private static final Set<String> CONTROLLERS_COM_IS_AUTHENTICATED = Set.of(
            // billing: autorização de cobrança feita pelo BillingGuardInterceptor
            "BillingController",
            // billing: administração com regras próprias de plano/role no serviço
            "BillingAdminController",
            // billing: visão administrativa com regras próprias no serviço
            "BillingAdminOverviewController",
            // company: isolamento da própria empresa por @CurrentCompanyId
            "CompanyController",
            // modules: módulos da própria empresa validados no serviço
            "CompanyModulesController",
            // convites: aceite e recusa por token, com usuário autenticado
            "InvitationController",
            // convites: aceite de convite de empresa por token, com usuário autenticado
            "CompanyInvitationController",
            // me: dados da conta do próprio usuário
            "MeController",
            // membership: associação do próprio usuário à empresa
            "MembershipController",
            // onboarding: conclusão do onboarding da própria conta
            "OnboardingController",
            // export: exportação disponível a qualquer membro autenticado da empresa
            "ReportExportController",
            // storage: arquivos da própria empresa com validação no serviço
            "StorageController",
            // TODO revisar: fora das categorias do plano (billing/me/onboarding/convites/company/storage/export/modules/membership); qualquer membro pode sugerir
            "ReportSuggestionController"
    );

    private static final Set<String> CONTROLLERS_COM_MISTURA_DE_MODULOS = Set.of(
            // company:update configura o SMTP e membership:manage consulta se há e-mail antes de convidar
            "CompanyMailController",
            // bairro novo pode nascer no cadastro da pessoa (person:update) ou na tela de localidades
            "LocationController"
    );

    @ArchTest
    static final ArchRule endpointsProtegidos = ArchRuleDefinition.methods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .and().arePublic()
            .and(ENDPOINT_HTTP)
            .should(beProtected());

    @ArchTest
    static final ArchRule isAuthenticatedRestritoAsControllersDeIdentidade = ArchRuleDefinition.methods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .should(isAuthenticatedApenasNosControllersDaAllowlist());

    @ArchTest
    static final ArchRule semMisturaDePrefixosDeModulosEmOr = ArchRuleDefinition.methods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .should(naoMisturarPrefixosDeModulos());

    private static ArchCondition<JavaMethod> beProtected() {
        return new ArchCondition<>("ter @PreAuthorize no método ou na classe") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                String alvo = chave(method);
                if (ENDPOINTS_SEM_PREAUTHORIZE.contains(alvo)) {
                    return;
                }
                if (preAuthorizeValue(method).isBlank()) {
                    events.add(SimpleConditionEvent.violated(method,
                            "Endpoint sem @PreAuthorize: " + method.getFullName() + " (" + alvo + ")"));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> isAuthenticatedApenasNosControllersDaAllowlist() {
        return new ArchCondition<>("usar isAuthenticated() apenas nos controllers de identidade/conta") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (!preAuthorizeValue(method).contains("isAuthenticated()")) {
                    return;
                }
                String controller = method.getOwner().getSimpleName();
                if (!CONTROLLERS_COM_IS_AUTHENTICATED.contains(controller)) {
                    events.add(SimpleConditionEvent.violated(method,
                            "isAuthenticated() fora da allowlist: " + method.getFullName()));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> naoMisturarPrefixosDeModulos() {
        return new ArchCondition<>("não misturar prefixos de módulos em 'or hasAuthority'") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                String expressao = preAuthorizeValue(method);
                if (!expressao.contains(" or ")) {
                    return;
                }
                Set<String> prefixos = new HashSet<>();
                Matcher authorities = HAS_AUTHORITY.matcher(expressao);
                while (authorities.find()) {
                    Matcher modulo = PREFIXO_MODULO.matcher(authorities.group(1));
                    if (modulo.find()) {
                        prefixos.add(modulo.group(1));
                    }
                }
                String controller = method.getOwner().getSimpleName();
                if (prefixos.size() > 1 && !CONTROLLERS_COM_MISTURA_DE_MODULOS.contains(controller)) {
                    events.add(SimpleConditionEvent.violated(method,
                            "Prefixos de módulos misturados em or hasAuthority: " + method.getFullName()
                                    + " -> " + prefixos));
                }
            }
        };
    }

    private static String preAuthorizeValue(JavaMethod method) {
        return method.tryGetAnnotationOfType(PreAuthorize.class)
                .map(PreAuthorize::value)
                .filter(value -> !value.isBlank())
                .or(() -> method.getOwner().tryGetAnnotationOfType(PreAuthorize.class).map(PreAuthorize::value))
                .orElse("");
    }

    private static String chave(JavaMethod method) {
        return method.getOwner().getSimpleName() + "#" + method.getName();
    }
}
