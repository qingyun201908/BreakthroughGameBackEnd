package BreakthroughGame.backEnd.UserModule.UserRestful.response;

public class AllResponse {
    public boolean success;
    public String message;


    public AllResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AllResponse() {

    }


    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
