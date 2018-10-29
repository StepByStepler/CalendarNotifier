import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws SQLException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        final String login = "gpanko@bk.ru";
        final String password = "Grisha123";

        Session session = Session.getInstance(getGmailProperties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(login, password);
            }
        });

        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/calendar?useSSH=false", "root", "root");
        PreparedStatement selectEmailReceivers = connection.prepareStatement(
                                                "select email, date_from, date_to, info " +
                                                     "from dates d, accounts acc " +
                                                     "where d.account_id = acc.id and d.date_from = ?");

        ResultSet emailReceivers = selectEmailReceivers.executeQuery();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");

        while(emailReceivers.next()) {
            try {
                MimeMessage message = new MimeMessage(session);

                message.setFrom(new InternetAddress(login));
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(emailReceivers.getString("email")));
                message.setSubject("Уведомление о вашем событии");
                message.setText(String.format("Ваше событие %s начинается сейчас, в %s и продлится до %s",
                        emailReceivers.getString("info"), dateFormat.format(emailReceivers.getTimestamp("date_from")),
                        dateFormat.format(emailReceivers.getTimestamp("date_to"))));

                Transport.send(message);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        connection.close();
    }

    private static Properties getGmailProperties() {
        Properties properties = new Properties();

        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        return properties;
    }


}
