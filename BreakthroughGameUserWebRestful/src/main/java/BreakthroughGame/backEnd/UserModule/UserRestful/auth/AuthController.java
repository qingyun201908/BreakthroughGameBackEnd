// 文件：AuthController.java（节选，核心改动）
package BreakthroughGame.backEnd.UserModule.UserRestful.auth;

import BreakthroughGame.backEnd.UserModule.UserInfo.entity.GameCharacter;
import BreakthroughGame.backEnd.UserModule.UserInfo.entity.User;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.CharacterDTO;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.AllResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.request.LoginRequest;
import BreakthroughGame.backEnd.UserModule.UserRestful.response.LoginResponse;
import BreakthroughGame.backEnd.UserModule.UserRestful.request.RegisterRequest;
import BreakthroughGame.backEnd.UserModule.UserRestful.service.CharacterService;
import BreakthroughGame.backEnd.UserModule.UserRestful.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "认证相关接口")
@CrossOrigin(
        origins = { "http://localhost:9000", "http://127.0.0.1:9000","http://127.0.0.1:5173","http://localhost:5173" },
        maxAge = 3600,
        methods = { RequestMethod.POST, RequestMethod.OPTIONS }
)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final CharacterService characterService;

    public AuthController(UserService userService, CharacterService characterService) { this.userService = userService;
        this.characterService = characterService;
    }

    @Operation(summary = "用户注册", description = "使用用户名、邮箱与密码注册新用户")
    @ApiResponse(responseCode = "201", description = "注册成功")
    @ApiResponse(responseCode = "409", description = "用户名或邮箱已存在",
            content = @Content(mediaType = "text/plain"))
    @PostMapping("/register")
    public Object register(@Valid @RequestBody RegisterRequest req) {
        LOGGER.info("[REGISTER] username={}, email={}", req.getUsername(), req.getEmail());

        // 中文备注：业务异常直接抛 BizException，由全局异常处理成统一返回
        User u = userService.register(req.getUsername(), req.getEmail(), req.getPassword());
        // 中文备注：为了简洁，这里直接返回字符串；会被 ResponseBodyAdvice 包装成 ApiResult
        return new AllResponse(true, "用户注册成功");
    }

    @Operation(summary = "用户登录", description = "用邮箱和密码登录")
    @ApiResponse(responseCode = "200", description = "登录成功",
            content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @ApiResponse(responseCode = "401", description = "邮箱或密码错误",
            content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        LOGGER.info("[LOGIN] email={}", req.getEmail());

        boolean ok = userService.authenticate(req.getEmail(), req.getPassword());
        if (!ok) {
            // 中文备注：不暴露账号是否存在；抛业务异常（或直接返回失败对象也行，但推荐异常集中处理）
            throw new BizException(ResultCode.A_UNAUTHORIZED, "邮箱或密码错误");
        }
        User user = userService.getByEmailOrThrow(req.getEmail()); // 中文备注：从邮箱拿用户
        GameCharacter character = characterService.createIfAbsent(user.getId()); // 中文备注：若无则创建，保证前端一定有角色
        // ✅ 新增：封装角色快照到响应
        CharacterDTO characterDTO = CharacterDTO.from(character);
        // 中文备注：正常返回 DTO，会被自动包装为 ApiResult<LoginResponse>
        return new LoginResponse(true, "登录成功",characterDTO);
    }
}
