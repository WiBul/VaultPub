package com.example.thevault.service;

import com.example.thevault.domain.mapping.dao.AssetDAO;
import com.example.thevault.domain.mapping.dao.KlantDAO;
import com.example.thevault.domain.mapping.dao.RekeningDAO;
import com.example.thevault.domain.mapping.repository.RootRepository;
import com.example.thevault.domain.model.Asset;
import com.example.thevault.domain.model.Cryptomunt;
import com.example.thevault.domain.model.Klant;
import com.example.thevault.domain.transfer.AssetDto;
import com.example.thevault.support.BSNvalidator;
import com.example.thevault.support.exceptions.AgeTooLowException;
import com.example.thevault.support.exceptions.IncorrectBSNException;
import com.example.thevault.support.exceptions.PasswordNotSuitableException;
import com.example.thevault.support.hashing.BCryptWachtwoordHash;
import com.example.thevault.support.hashing.HashHelper;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;


class KlantServiceTest {


    private static Klant testKlant;
    private static Klant andereKlant;
    private static KlantService klantService;
    private static RootRepository mockRootRepository;
    public static List<Asset> portefeuille;
    public static List<AssetDto> portefeuilleDto;
    public static Cryptomunt testCryptomunt1;
    public static Cryptomunt testCryptomunt2;
    public static Cryptomunt testCryptomunt3;
    public static Asset testAsset1;
    public static Asset testAsset2;
    public static Asset testAsset3;
    public static Asset testAsset4;
    public static AssetDto testAssetDto1;
    public static AssetDto testAssetDto2;
    public static AssetDto testAssetDto3;
    public static AssetDto testAssetDto4;


    @BeforeEach
    void setUp() {
        mockRootRepository = Mockito.mock(RootRepository.class);
        String gehashtWachtwoord = BCryptWachtwoordHash.hashWachtwoord("testWW");
        testKlant = new Klant("testKlant", gehashtWachtwoord, // klant is exact 18 vandaag
                null, null, "Jan", null, BSNvalidator.TESTBSN_VAN_RIVG,
                LocalDate.now().minusYears(KlantService.VOLWASSEN_LEEFTIJD));
         klantService = new KlantService(mockRootRepository);
        andereKlant = new Klant("andereKlant", gehashtWachtwoord, // klant is exact 18 vandaag
                null, null, "Jan", null, BSNvalidator.TESTBSN_VAN_RIVG,
                LocalDate.now().minusYears(KlantService.VOLWASSEN_LEEFTIJD));
        /**
         * @Author Carmen
         * Hieronder alle noodzakelijke input voor de test van portefeuille DTOs
         */
        testCryptomunt1 = new Cryptomunt(1, "CarmenCrypto", "CCR", 100.0);
        testCryptomunt2 = new Cryptomunt(2, "DigiCrypto", "DIG", 75.0);
        testCryptomunt3 = new Cryptomunt(3, "Coyne", "COY", 125.0);
        testAsset1 = new Asset(testCryptomunt1, 5.1, LocalDateTime.now());
        testAsset2 = new Asset(testCryptomunt2, 2.4, LocalDateTime.now());
        testAsset3 = new Asset(testCryptomunt3, 3.6, LocalDateTime.now());
        testAsset4 = new Asset(testCryptomunt1, 0.5, LocalDateTime.now());
        testAssetDto1 = new AssetDto(testAsset1);
        testAssetDto2 = new AssetDto(testAsset2);
        testAssetDto3 = new AssetDto(testAsset3);
        testAssetDto4 = new AssetDto(testAsset4);
        portefeuille = new ArrayList<>();
        portefeuille.add(testAsset1);
        portefeuille.add(testAsset2);
        portefeuille.add(testAsset3);
        portefeuilleDto = new ArrayList<>();
        portefeuilleDto.add(testAssetDto1);
        portefeuilleDto.add(testAssetDto2);
        portefeuilleDto.add(testAssetDto3);
    }



    @Test
    void vindKlantByUsername() {
        Mockito.when(mockRootRepository.vindKlantByGebruikersnaam("testKlant"))
                .thenReturn(testKlant);
        Klant expected = testKlant;
        Klant actual = klantService.vindKlantByGebruikersnaam(testKlant.getGebruikersnaam());
        assertThat(actual).isNotNull().isEqualTo(expected);
        actual = klantService.vindKlantByGebruikersnaam(andereKlant.getGebruikersnaam());
        assertThat(actual).isNull();
    }

    /**
     * @Author Carmen
     * Omdat ik de methode heb verplaatst van AssetService naar KlantService moest ik ook de test verplaatsen
     */
    @Test
    void geefNuttigePortefeuille() {
        Mockito.when(mockRootRepository.vulPortefeuilleKlant(testKlant)).thenReturn(portefeuille);
        List<AssetDto> expected = portefeuilleDto;
        List<AssetDto> actual = klantService.geefNuttigePortefeuille(testKlant);
        assertThat(actual).as("Test geef inhoud portefeuilleDto van testklant").isNotNull().isEqualTo(expected).
                contains(testAssetDto1, atIndex(0)).contains(testAssetDto2, atIndex(1)).contains(testAssetDto3, atIndex(2)).
                doesNotContain(testAssetDto4).hasSize(3).extracting(AssetDto::getName).
                contains("CarmenCrypto", "DigiCrypto", "Coyne").doesNotContain("BitCoin");
    }

    @Test
    void registreerKlant() { // de a staat ervoor, zodat deze test als eerste gaat
        Mockito.when(mockRootRepository.slaKlantOp(testKlant)).thenReturn(testKlant);
        Klant expected = testKlant;
        Klant actual = klantService.registreerKlant(testKlant);
        assertThat(actual).isNotNull().isEqualTo(expected);
        testKlant.setGeboortedatum(testKlant.getGeboortedatum().plusDays(1)); // nu te jong om rekening te openen
        assertThatThrownBy(() -> klantService.registreerKlant(testKlant)).isInstanceOf(AgeTooLowException.class);
        testKlant.setGeboortedatum(testKlant.getGeboortedatum().minusDays(1));
        testKlant.setBsn(testKlant.getBsn() + 1);
        assertThatThrownBy(() -> klantService.registreerKlant(testKlant)).isInstanceOf(IncorrectBSNException.class);
        testKlant.setBsn(testKlant.getBsn() - 1);
        testKlant.setWachtwoord("WwMet Spatie");
        assertThat(klantService.checkWachtwoordFormat(testKlant)).isFalse();
        assertThatThrownBy(() -> klantService.registreerKlant(testKlant)).isInstanceOf(PasswordNotSuitableException.class);
        testKlant.setWachtwoord("Ww<8Kar");
        assertThat(klantService.checkWachtwoordLengte(testKlant)).isFalse();
        assertThatThrownBy(() -> klantService.registreerKlant(testKlant)).isInstanceOf(PasswordNotSuitableException.class);

    }
    @Test
    void checkVolwassen(){
        assertThat(klantService.checkVolwassen(testKlant)).isTrue();
        testKlant.setGeboortedatum(testKlant.getGeboortedatum().plusDays(1)); // nu te jong om rekening te openen
        assertThat(klantService.checkVolwassen(testKlant)).isFalse();
        testKlant.setGeboortedatum(testKlant.getGeboortedatum().minusDays(4));
        assertThat(klantService.checkVolwassen(testKlant)).isTrue();

    }
    @Test
    void checkWachtwoordFormat(){
        testKlant.setWachtwoord("WwMet Spatie");
        assertThat(klantService.checkWachtwoordFormat(testKlant)).isFalse();
        testKlant.setWachtwoord("WwZonderSpatie");
        assertThat(klantService.checkWachtwoordFormat(testKlant)).isTrue();


    }
    @Test
    void checkWachtwoordLengte(){
        testKlant.setWachtwoord("Ww<8Kar");
        assertThat(klantService.checkWachtwoordLengte(testKlant)).isFalse();
        testKlant.setWachtwoord("8KarInWw");
        assertThat(klantService.checkWachtwoordLengte(testKlant)).isTrue();
    }

}