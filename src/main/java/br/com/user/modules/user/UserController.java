package br.com.user.modules.user;

import br.com.user.modules.family.dto.FamilyMemberResponse;
import br.com.user.modules.user.dto.PasswordChangeRequest;
import br.com.user.modules.user.dto.UserResponse;
import br.com.user.modules.user.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getUser(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getUser(jwt));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateUser(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(jwt, request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @RequestBody PasswordChangeRequest request) {
        userService.changePassword(jwt, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lista")
    public ResponseEntity<List<FamilyMemberResponse>> getUsersWithoutFamily() {
        return ResponseEntity.ok(userService.getUsersWithoutFamily());
    }
}
