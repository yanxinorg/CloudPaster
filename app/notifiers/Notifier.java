package notifiers;
import models.Paster;
import play.Play;
import play.mvc.Mailer;

public class Notifier extends Mailer {
 
   public static void welcome(String email) {
      setSubject("Welcome %s", email);
      addRecipient(email);
      setFrom("Me <me@me.com>");
      addAttachment(Play.getFile("rules.pdf"));
      send(email);
   }
 
   
   public static void paste(String email,Paster paster) {
	   setSubject("Paster 更新提醒");
	   addRecipient(email);
	   setContentType("text/html");
	   setCharset("UTF-8");
	   setFrom(Play.configuration.getProperty("mail.smtp.from"));
	   String username = email.substring(0,email.indexOf("@"));
	   send(username,paster);
   }
   
   public static void lostPassword(String email) {
      setFrom("Robot <robot@thecompany.com>");
      setSubject("Your password has been reset");
      addRecipient(email);
   }
 
}
