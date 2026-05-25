package br.com.user.modules.family;

import br.com.user.modules.family.dto.CreateFamilyRequest;
import br.com.user.modules.family.dto.FamilyResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/user/family")
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    @PostMapping
    public ResponseEntity<FamilyResponse> createFamily(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody CreateFamilyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(familyService.createFamily(jwt, request.nome()));
    }

    @GetMapping
    public ResponseEntity<FamilyResponse> getFamily(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(familyService.getFamily(jwt));
    }

    @PostMapping("/members/{username}")
    public ResponseEntity<Void> addFamilyMember(@PathVariable String username,
                                                @AuthenticationPrincipal Jwt jwt) {
        familyService.addFamilyMember(jwt, username);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/members/{username}")
    public ResponseEntity<Void> removeFamilyMember(@PathVariable String username,
                                                   @AuthenticationPrincipal Jwt jwt) {
        familyService.removeFamilyMember(jwt, username);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteFamily(@AuthenticationPrincipal Jwt jwt) {
        familyService.deleteFamily(jwt);
        return ResponseEntity.noContent().build();
    }

}
