package me.t3sl4.hydraulic.Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import me.t3sl4.hydraulic.Launcher;
import me.t3sl4.hydraulic.Util.HTTP.HTTPRequest;
import static me.t3sl4.hydraulic.MainModel.Main.loggedInUser;
import me.t3sl4.hydraulic.Util.SceneUtil;
import me.t3sl4.hydraulic.Util.Data.User.User;
import me.t3sl4.hydraulic.Util.Util;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static me.t3sl4.hydraulic.Launcher.*;

public class LoginController implements Initializable {

    @FXML
    private Label lblErrors;

    @FXML
    private Label codedBy;

    @FXML
    private TextField txtUsername;

    @FXML
    private TextField txtPassword;

    @FXML
    private Button btnSignin;

    @FXML
    private Button btnSignup;

    @FXML
    private Button togglePasswordButton;
    @FXML
    private TextField sifrePassword;
    @FXML
    private ImageView passwordVisibilityIcon;
    @FXML
    private ToggleButton beniHatirla;

    private String girilenSifre = "";

    private static final Logger logger = Logger.getLogger(MainController.class.getName());

    @FXML
    public void girisYap(MouseEvent event) {
        if (Util.netIsAvailable()) {
            Stage stage = (Stage) btnSignin.getScene().getWindow();

            if (txtUsername.getText().isEmpty() || txtPassword.getText().isEmpty()) {
                Util.showErrorOnLabel(lblErrors, "Şifre veya kullanıcı adı girmediniz !");
            } else {
                String loginUrl = BASE_URL + loginURLPrefix;
                String jsonLoginBody = "{\"Username\": \"" + txtUsername.getText() + "\", \"Password\": \"" + txtPassword.getText() + "\"}";

                HTTPRequest.sendRequest(loginUrl, jsonLoginBody, new HTTPRequest.RequestCallback() {
                    @Override
                    public void onSuccess(String loginResponse) {
                        String profileInfoUrl = BASE_URL + profileInfoURLPrefix +":Role";
                        String jsonProfileInfoBody = "{\"Username\": \"" + txtUsername.getText() + "\"}";
                        HTTPRequest.sendRequest(profileInfoUrl, jsonProfileInfoBody, new HTTPRequest.RequestCallback() {
                            @Override
                            public void onSuccess(String profileInfoResponse) {
                                JSONObject roleObject = new JSONObject(profileInfoResponse);
                                String roleValue = roleObject.getString("Role");
                                if (roleValue.equals("TECHNICIAN") || roleValue.equals("ENGINEER") || roleValue.equals("SYSOP")) {
                                    loggedInUser = new User(txtUsername.getText());

                                    updateUserAndOpenMainScreen(stage);
                                    beniHatirla();
                                } else {
                                    Util.showErrorOnLabel(lblErrors, "Hidrolik aracını normal kullanıcılar kullanamaz.");
                                }
                            }

                            @Override
                            public void onFailure() {
                                Util.showErrorOnLabel(lblErrors, "Profil bilgileri alınamadı!");
                            }
                        });
                    }

                    @Override
                    public void onFailure() {
                        Util.showErrorOnLabel(lblErrors, "Kullanıcı adı veya şifre hatalı !");
                        removeBeniHatirla();
                    }
                });
            }
        } else {
            Util.showErrorOnLabel(lblErrors, "Lütfen internet bağlantınızı kontrol edin!");
        }
    }

    @FXML
    public void kayitOl() {
        if (Util.netIsAvailable()) {
            Stage stage = (Stage) btnSignup.getScene().getWindow();

            stage.close();

            try {
                openRegisterScreen();
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            Util.showErrorOnLabel(lblErrors, "Lütfen internet bağlantınızı kontrol edin!");
        }
    }

    @FXML
    public void sifremiUnuttum() throws IOException {
        Stage stage = (Stage) btnSignin.getScene().getWindow();

        stage.close();
        openResetPasswordScreen();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        codedBy.setText("Designed and Coded by\nHalil İbrahim Direktör");
        if(Util.netIsAvailable()) {
            beniHatirlaKontrol();
            girisKontrol();
            beniHatirla();
        } else {
            Util.showErrorOnLabel(lblErrors, "Lütfen internet bağlantınızı kontrol edin!");
        }
        togglePasswordButton.setOnMouseClicked(event -> togglePasswordVisibility());
        txtPassword.textProperty().addListener((observable, oldValue, newValue) -> {
            girilenSifre = newValue;
        });
    }

    public void beniHatirla() {
        beniHatirla.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                String getCipheredPassUrl = BASE_URL + getPassURLPrefix;
                String jsonGetCipheredPassBody = "{\"Password\": \"" + txtPassword.getText() + "\"}";

                String username = txtUsername.getText().trim();

                HTTPRequest.sendRequest(getCipheredPassUrl, jsonGetCipheredPassBody, new HTTPRequest.RequestCallback() {
                    @Override
                    public void onSuccess(String getPassResponse) {
                        JSONObject passObject = new JSONObject(getPassResponse);
                        String password = passObject.getString("pass");
                        if (!username.isEmpty() && password != null && !password.isEmpty()) {
                            try {
                                FileWriter writer = new FileWriter(Launcher.loginFilePath + "loginInfo.txt");
                                writer.write(username + "\n");
                                writer.write(password);
                                writer.close();
                            } catch (IOException e) {
                                logger.log(Level.SEVERE, e.getMessage(), e);
                            }
                        } else {
                            Util.showErrorMessage("Kullanıcı adı veya şifre alanları boş olamaz!");
                            beniHatirla.setSelected(false);
                        }
                    }

                    @Override
                    public void onFailure() {
                        Util.showErrorOnLabel(lblErrors, "Hashlenmiş şifre alınamadı!");
                        beniHatirla.setSelected(false);
                    }
                });
            } else {
                File loginFile = new File(Launcher.loginFilePath + "loginInfo.txt");
                if (loginFile.exists()) {
                    if (loginFile.delete()) {
                        System.out.println("loginInfo.txt dosyası silindi.");
                    } else {
                        System.err.println("loginInfo.txt dosyası silinemedi.");
                    }
                }
            }
        });
    }

    private void removeBeniHatirla() {
        File loginFile = new File(Launcher.loginFilePath + "loginInfo.txt");
        if(loginFile.exists()) {
            loginFile.delete();
        }
        beniHatirla.setSelected(false);
    }

    private void beniHatirlaKontrol() {
        File loginFile = new File(Launcher.loginFilePath + "loginInfo.txt");

        if (loginFile.exists()) {
            beniHatirla.setSelected(true);
        } else {
            beniHatirla.setSelected(false);
        }
    }

    private void girisKontrol() {
        String kullaniciAdi = null;
        String sifre = null;
        if (beniHatirla.isSelected()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(Launcher.loginFilePath))) {
                kullaniciAdi = reader.readLine();
                sifre = reader.readLine();
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }

            if(Util.netIsAvailable()) {
                directLogin(kullaniciAdi, sifre);
            } else {
                File forceDelete = new File(Launcher.loginFilePath);
                forceDelete.delete();
                beniHatirla.setSelected(false);
                Util.showErrorOnLabel(lblErrors, "Lütfen internet bağlantınızı kontrol edin !");
            }
        }
    }

    private void directLogin(String kullaniciAdi, String sifre) {
        String loginUrl = BASE_URL + directLoginURLPrefix;
        String jsonLoginBody = "{\"Username\": \"" + kullaniciAdi + "\", \"Password\": \"" + sifre + "\"}";

        HTTPRequest.sendRequest(loginUrl, jsonLoginBody, new HTTPRequest.RequestCallback() {
            @Override
            public void onSuccess(String loginResponse) {
                String profileInfoUrl = BASE_URL + profileInfoURLPrefix +":Role";
                String jsonProfileInfoBody = "{\"Username\": \"" + kullaniciAdi + "\"}";
                HTTPRequest.sendRequest(profileInfoUrl, jsonProfileInfoBody, new HTTPRequest.RequestCallback() {
                    @Override
                    public void onSuccess(String profileInfoResponse) {
                        JSONObject roleObject = new JSONObject(profileInfoResponse);
                        String roleValue = roleObject.getString("Role");
                        if (roleValue.equals("TECHNICIAN") || roleValue.equals("ENGINEER") || roleValue.equals("SYSOP")) {
                            loggedInUser = new User(kullaniciAdi);

                            updateUser(() -> {
                                try {
                                    openMainScreen();
                                } catch (IOException e) {
                                    logger.log(Level.SEVERE, e.getMessage(), e);
                                }
                                Stage loginStage = (Stage) btnSignin.getScene().getWindow();
                                loginStage.close();
                            });
                        } else {
                            Util.showErrorOnLabel(lblErrors, "Hidrolik aracını normal kullanıcılar kullanamaz.");
                        }
                    }

                    @Override
                    public void onFailure() {
                        Util.showErrorOnLabel(lblErrors, "Profil bilgileri alınamadı!");
                    }
                });
            }

            @Override
            public void onFailure() {
                Util.showErrorOnLabel(lblErrors, "Kullanıcı adı veya şifre hatalı !");
            }
        });
    }

    private void togglePasswordVisibility() {
        if (txtPassword.isVisible()) {
            txtPassword.setManaged(false);
            txtPassword.setVisible(false);
            sifrePassword.setManaged(true);
            sifrePassword.setVisible(true);
            sifrePassword.setText(girilenSifre);
            passwordVisibilityIcon.setImage(new Image(Objects.requireNonNull(Launcher.class.getResourceAsStream("icons/hidePass.png"))));
        } else {
            txtPassword.setManaged(true);
            txtPassword.setVisible(true);
            sifrePassword.setManaged(false);
            sifrePassword.setVisible(false);
            passwordVisibilityIcon.setImage(new Image(Objects.requireNonNull(Launcher.class.getResourceAsStream("icons/showPass.png"))));
        }
    }

    @FXML
    public void openGithub() {
        Util.openURL("https://github.com/hidirektor");
    }

    @FXML
    public void openOnder() {
        Util.openURL("https://ondergrup.com");
    }

    @FXML
    public void ekraniKapat() {
        Stage stage = (Stage) btnSignin.getScene().getWindow();

        stage.close();
    }

    private void openMainScreen() throws IOException {
        SceneUtil.changeScreen("fxml/Home.fxml");
    }

    private void openRegisterScreen() throws IOException {
        SceneUtil.changeScreen("fxml/Register.fxml");
    }

    private void openResetPasswordScreen() throws IOException {
        SceneUtil.changeScreen("fxml/ResetPassword.fxml");
    }

    public void updateUserAndOpenMainScreen(Stage stage) {
        updateUser(() -> {
            try {
                openMainScreen();
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
            stage.close();
        });
    }

    public void updateUser(Runnable onUserUpdateComplete) {
        String profileInfoUrl = BASE_URL + wholeProfileURLPrefix;
        String profileInfoBody = "{\"username\": \"" + loggedInUser.getUsername() + "\"}";

        HTTPRequest.sendRequest(profileInfoUrl, profileInfoBody, new HTTPRequest.RequestCallback() {
            @Override
            public void onSuccess(String profileInfoResponse) {
                JSONObject responseJson = new JSONObject(profileInfoResponse);
                String parsedRole = responseJson.getString("Role");
                String parsedFullName = responseJson.getString("NameSurname");
                String parsedEmail = responseJson.getString("Email");
                String parsedPhone = responseJson.getString("Phone");
                String parsedCompanyName = responseJson.getString("CompanyName");

                loggedInUser.setRole(parsedRole);
                loggedInUser.setFullName(parsedFullName);
                loggedInUser.setEmail(parsedEmail);
                loggedInUser.setPhone(parsedPhone);
                loggedInUser.setCompanyName(parsedCompanyName);
                onUserUpdateComplete.run();
            }

            @Override
            public void onFailure() {
                System.out.println("Kullanıcı bilgileri alınamadı!");
            }
        });
    }
}