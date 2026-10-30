package lk.swiftdrop.merchant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.merchant.dto.MerchantResponse;
import lk.swiftdrop.merchant.dto.RegisterMerchantRequest;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.entity.MerchantStatus;
import lk.swiftdrop.merchant.repository.MerchantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private MerchantService merchantService;

    @Test
    @DisplayName("Merchant registration succeeds with status PENDING")
    void registerMerchantSuccess() {
        RegisterMerchantRequest request = new RegisterMerchantRequest(
                "Apex Electronics", "BR-12345", "Nimal Perera",
                "nimal@apex.lk", "+94771234567", "123 Galle Road, Colombo 03"
        );
        UUID userId = UUID.randomUUID();

        when(merchantRepository.existsByBusinessRegNo("BR-12345")).thenReturn(false);
        when(merchantRepository.existsByEmail("nimal@apex.lk")).thenReturn(false);
        when(merchantRepository.existsByUserId(userId)).thenReturn(false);
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MerchantResponse response = merchantService.register(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.businessName()).isEqualTo("Apex Electronics");
        assertThat(response.status()).isEqualTo(MerchantStatus.PENDING);
        assertThat(response.userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Registration fails when business registration number already exists")
    void registerDuplicateBusinessRegNo() {
        RegisterMerchantRequest request = new RegisterMerchantRequest(
                "Apex Electronics", "BR-DUPLICATE", "Nimal Perera",
                "nimal@apex.lk", "+94771234567", "123 Galle Road, Colombo 03"
        );

        when(merchantRepository.existsByBusinessRegNo("BR-DUPLICATE")).thenReturn(true);

        assertThatThrownBy(() -> merchantService.register(request, UUID.randomUUID()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Business registration number is already registered");
    }

    @Test
    @DisplayName("Registration fails when email already exists")
    void registerDuplicateEmail() {
        RegisterMerchantRequest request = new RegisterMerchantRequest(
                "Apex Electronics", "BR-NEW", "Nimal Perera",
                "existing@apex.lk", "+94771234567", "123 Galle Road, Colombo 03"
        );

        when(merchantRepository.existsByBusinessRegNo("BR-NEW")).thenReturn(false);
        when(merchantRepository.existsByEmail("existing@apex.lk")).thenReturn(true);

        assertThatThrownBy(() -> merchantService.register(request, UUID.randomUUID()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email is already registered");
    }

    @Test
    @DisplayName("Registration fails when user already registered a merchant account")
    void registerDuplicateUserId() {
        RegisterMerchantRequest request = new RegisterMerchantRequest(
                "Apex Electronics", "BR-NEW", "Nimal Perera",
                "nimal@apex.lk", "+94771234567", "123 Galle Road, Colombo 03"
        );
        UUID userId = UUID.randomUUID();

        when(merchantRepository.existsByBusinessRegNo("BR-NEW")).thenReturn(false);
        when(merchantRepository.existsByEmail("nimal@apex.lk")).thenReturn(false);
        when(merchantRepository.existsByUserId(userId)).thenReturn(true);

        assertThatThrownBy(() -> merchantService.register(request, userId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("User already has a registered merchant account");
    }

    @Test
    @DisplayName("Admin can approve merchant registration")
    void approveMerchant() {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, UUID.randomUUID(), "Apex Store", "BR-1",
                "Nimal", "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.PENDING);

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MerchantResponse response = merchantService.approveMerchant(merchantId);

        assertThat(response.status()).isEqualTo(MerchantStatus.APPROVED);
        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.APPROVED);
        assertThat(merchant.getRejectionReason()).isNull();
    }

    @Test
    @DisplayName("Admin can reject merchant registration with reason")
    void rejectMerchant() {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, UUID.randomUUID(), "Apex Store", "BR-1",
                "Nimal", "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.PENDING);

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MerchantResponse response = merchantService.rejectMerchant(merchantId, "Incomplete documentation");

        assertThat(response.status()).isEqualTo(MerchantStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Incomplete documentation");
        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.REJECTED);
    }

    @Test
    @DisplayName("Lookup by ID throws NotFoundException when merchant does not exist")
    void getMerchantByIdNotFound() {
        UUID randomId = UUID.randomUUID();
        when(merchantRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.getMerchantById(randomId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Merchant not found");
    }

    @Test
    @DisplayName("Lookup by User ID returns merchant profile")
    void getMerchantByUserIdSuccess() {
        UUID userId = UUID.randomUUID();
        Merchant merchant = new Merchant(UUID.randomUUID(), userId, "Apex Store", "BR-1",
                "Nimal", "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(merchantRepository.findByUserId(userId)).thenReturn(Optional.of(merchant));

        MerchantResponse response = merchantService.getMerchantByUserId(userId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.businessName()).isEqualTo("Apex Store");
    }

    @Test
    @DisplayName("List merchants by status returns page of merchants")
    void listMerchantsByStatus() {
        Merchant merchant = new Merchant(UUID.randomUUID(), UUID.randomUUID(), "Apex Store", "BR-1",
                "Nimal", "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.PENDING);
        Page<Merchant> page = new PageImpl<>(List.of(merchant));

        when(merchantRepository.findAllByStatus(MerchantStatus.PENDING, PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<MerchantResponse> result = merchantService.listMerchantsByStatus(MerchantStatus.PENDING, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(MerchantStatus.PENDING);
    }
}
