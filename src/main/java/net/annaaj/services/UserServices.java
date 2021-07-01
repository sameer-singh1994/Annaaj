package net.annaaj.services;

import java.io.UnsupportedEncodingException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import net.annaaj.Models.User;
import net.annaaj.repositories.UserRepository;
import net.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UserServices {

  @Autowired
  private UserRepository repo;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JavaMailSender mailSender;

  public void register(User user, String siteURL)
      throws UnsupportedEncodingException, MessagingException {
    String encodedPassword = passwordEncoder.encode(user.getPassword());
    user.setPassword(encodedPassword);

    String randomCode = RandomString.make(64);
    user.setVerificationCode(randomCode);
    user.setEnabled(false);

    repo.save(user);

    sendVerificationEmail(user, siteURL);
  }

  private void sendVerificationEmail(User user, String siteURL)
      throws MessagingException, UnsupportedEncodingException {
    String toAddress = user.getEmail();
    String fromAddress = "support@annaaj.com";
    String senderName = "Annaaj";
    String subject = "Please verify your registration";
    String content = "Dear [[name]],<br>"
        + "Please click the link below to verify your registration:<br>"
        + "<h3><a href=\"[[URL]]\" target=\"_self\">Tasty and healthy food loading... (click here)</a></h3>"
        + "Thank you,<br>"
        + "The Annaaj team.";

    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message);

    helper.setFrom(fromAddress, senderName);
    helper.setTo(toAddress);
    helper.setSubject(subject);

    content = content.replace("[[name]]", user.getFullName());
    String verifyURL = siteURL + "/verify?code=" + user.getVerificationCode();

    content = content.replace("[[URL]]", verifyURL);

    helper.setText(content, true);

    mailSender.send(message);

  }

  public boolean verify(String verificationCode) {
    User user = repo.findByVerificationCode(verificationCode);

    if (user == null || user.isEnabled()) {
      return false;
    } else {
      user.setVerificationCode(null);
      user.setEnabled(true);
      repo.save(user);

      return true;
    }
  }

}
