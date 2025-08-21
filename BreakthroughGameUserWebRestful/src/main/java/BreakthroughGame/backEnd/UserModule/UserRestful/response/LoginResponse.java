package BreakthroughGame.backEnd.UserModule.UserRestful.response;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.CharacterDTO;

public class LoginResponse extends AllResponse{
    private CharacterDTO character; // 新增：人物信息

    public LoginResponse() {
        super();
    }
    public LoginResponse(boolean success, String message) {
        super(success, message);
    }

    public LoginResponse(boolean success, String message, CharacterDTO character) {
        this.success = success;
        this.message = message;
        this.character = character;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public CharacterDTO getCharacter() { return character; }
    public void setCharacter(CharacterDTO character) { this.character = character; }
}

