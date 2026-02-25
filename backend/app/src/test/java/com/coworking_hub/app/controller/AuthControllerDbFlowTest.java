package com.coworking_hub.app.controller;

import com.coworking_hub.app.model.Korisnik;
import com.coworking_hub.app.model.enums.StatusKorisnika;
import com.coworking_hub.app.repository.KorisnikRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authflow;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.access-token-expiration-seconds=3600",
        "app.upload.base-dir=uploads-test",
        "app.upload.public-prefix=/uploads"
})
class AuthControllerDbFlowTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private KorisnikRepository korisnikRepository;

    @Test
    void registerThenApproveThenLoginShouldWork() {
        AuthController.RegisterMemberRequest registerRequest = new AuthController.RegisterMemberRequest(
                "pera",
                "Pera123!",
                "Pera",
                "Peric",
                "+38160123456",
                "pera@example.com"
        );

        ResponseEntity<AuthController.RegistrationResponse> registerResponse =
                authController.registerMember(registerRequest, null);

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());
        assertEquals("pera", registerResponse.getBody().username());

        Korisnik korisnik = korisnikRepository.findByKorisnickoIme("pera").orElseThrow();
        korisnik.setStatus(StatusKorisnika.odobren);
        korisnikRepository.save(korisnik);

        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest("pera", "Pera123!");
        ResponseEntity<?> loginResponse = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        AuthController.LoginResponse body = assertInstanceOf(AuthController.LoginResponse.class, loginResponse.getBody());
        assertNotNull(body.accessToken());
        assertEquals("Bearer", body.tokenType());
        assertEquals("pera", body.user().username());
    }
}
