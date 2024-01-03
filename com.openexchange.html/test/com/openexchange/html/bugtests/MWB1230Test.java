package com.openexchange.html.bugtests;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.openexchange.html.AbstractSanitizing;

public class MWB1230Test extends AbstractSanitizing {

    /**
     * Initializes a new {@link MWB1230Test}.
     */
    public MWB1230Test() {
        super();
    }

    @Test
    public void testHtml2Text() {
        String content = "<!DOCTYPE html>\n"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
            + "   <head>\n"
            + "      <title>GroFi E-Mail-Archivierung</title>\n"
            + "   </head>\n"
            + "   <body style=\"background-color:#fff;font-family: &#39;Open Sans&#39;, sans-serif;font-size: 14px;color: #707070;padding: 35px 0 35px 0;\">\n"
            + "      <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"text-align:center;\" width=\"100%\">\n"
            + "         <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse; width:100%; border-spacing:0\" width=\"100%\">\n"
            + "            <tbody>\n"
            + "               <tr valign=\"top\">\n"
            + "                  <td align=\"left\" style=\"padding:0px; min-width:640px; width:640px\" width=\"640\">\n"
            + "                     <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse; width:100%; border-spacing:0\" width=\"100%\">\n"
            + "                        <tbody>\n"
            + "                           <tr valign=\"top\">\n"
            + "                              <td align=\"left\" bgcolor=\"#f8f8f8\" style=\"padding:0px; background-color:#f8f8f8\">\n"
            + "                                 <table bgcolor=\"#003d8f\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse; background-color:#003d8f; width:100%; border-spacing:0\" width=\"100%\">\n"
            + "                                    <tbody>\n"
            + "                                       <tr valign=\"top\">\n"
            + "                                          <td align=\"left\" style=\"padding:0px\">\n"
            + "                                             <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse\">\n"
            + "                                                <tbody>\n"
            + "                                                   <tr>\n"
            + "                                                      <td height=\"20\" style=\"padding:0px; width:1px; height:20px; line-height:20px\" width=\"1\"><img alt=\"\" border=\"0\" data-imagetype=\"External\" height=\"20\" src=\"https://simg.grofi.com/2018/ONE/11/SPIN684-CUCO-18897-DE/px.gif\" style=\"display:block; width:1px; height:20px; border:0; visibility:hidden\" width=\"1\" /></td>\n"
            + "                                                   </tr>\n"
            + "                                                </tbody>\n"
            + "                                             </table>\n"
            + "                                             <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse; width:100%; border-spacing:0\" width=\"100%\">\n"
            + "                                                <tbody>\n"
            + "                                                   <tr valign=\"top\">\n"
            + "                                                      <td style=\"padding:0px; min-width:25px; width:25px; line-height:1px; font-size:0px\" width=\"25\"><img alt=\"\" border=\"0\" data-imagetype=\"External\" height=\"1\" src=\"https://simg.grofi.com/2018/ONE/11/SPIN684-CUCO-18897-DE/px.gif\" style=\"display:block; width:25px; height:1px; border:0; visibility:hidden\" width=\"25\" /></td>\n"
            + "                                                      <td align=\"left\" height=\"55\" style=\"padding:0px; min-width:190px; width:190px; min-height:55px; height:55px\" width=\"190\"><img alt=\"GroFi\" border=\"0\" data-imagetype=\"External\" height=\"auto\" src=\"https://e-mail-archiv.grofi.de/images/1n1/email-logo-@2x.png\" style=\"color:#50575b; margin:0; border:0; font-size:13px; font-family:&#39;Open Sans&#39;,Arial,sans-serif; line-height:0; display:block; width:100%; height:auto; padding:0\" width=\"100%\" /></td>\n"
            + "                                                      <td align=\"right\" style=\"padding:0px; font-size:18px; line-height:22px\" valign=\"bottom\"><span style=\"color:#ffffff; font-family:&#39;Open Sans&#39;,Arial,sans-serif; font-weight:bold; font-size:18px; line-height:22px\"></span></td>\n"
            + "                                                      <td style=\"padding:0px; min-width:25px; width:25px; line-height:1px; font-size:0px\" width=\"25\"><img alt=\"\" border=\"0\" data-imagetype=\"External\" height=\"1\" src=\"https://simg.grofi.com/2018/ONE/11/SPIN684-CUCO-18897-DE/px.gif\" style=\"display:block; width:25px; height:1px; border:0; visibility:hidden\" width=\"25\" /></td>\n"
            + "                                                   </tr>\n"
            + "                                                </tbody>\n"
            + "                                             </table>\n"
            + "                                             <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse\">\n"
            + "                                                <tbody>\n"
            + "                                                   <tr>\n"
            + "                                                      <td height=\"15\" style=\"padding:0px; width:1px; height:15px; line-height:15px\" width=\"1\"><img alt=\"\" border=\"0\" data-imagetype=\"External\" height=\"15\" src=\"https://simg.grofi.com/2018/ONE/11/SPIN684-CUCO-18897-DE/px.gif\" style=\"display:block; width:1px; height:15px; border:0; visibility:hidden\" width=\"1\" /></td>\n"
            + "                                                   </tr>\n"
            + "                                                </tbody>\n"
            + "                                             </table>\n"
            + "                                          </td>\n"
            + "                                       </tr>\n"
            + "                                    </tbody>\n"
            + "                                 </table>\n"
            + "                              </td>\n"
            + "                           </tr>\n"
            + "                           <tr valign=\"top\">\n"
            + "                              <td align=\"left\" bgcolor=\"#f8f8f8\" style=\"padding:0px; background-color:#f8f8f8\">\n"
            + "                                 <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse; border-spacing:0\">\n"
            + "                                    <tbody>\n"
            + "                                       <tr valign=\"top\">\n"
            + "                                          <td align=\"left\" height=\"180\" style=\"padding:0px; min-width:640px; width:640px; min-height:180px; height:180px\" width=\"640\"><img alt=\"\" border=\"0\" data-imagetype=\"External\" height=\"180\" src=\"https://e-mail-archiv.grofi.de/images/1n1/DEU-mailer-banner.png\" style=\"color:#50575b; margin:0; border:0; font-size:13px; font-family:&#39;Open Sans&#39;,Arial,sans-serif; line-height:0; display:block; width:640px; height:180px; padding:0\" width=\"640\" /></td>\n"
            + "                                       </tr>\n"
            + "                                    </tbody>\n"
            + "                                 </table>\n"
            + "                                 <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse\">\n"
            + "                                    <tbody>\n"
            + "                                       <tr>\n"
            + "                                          <td height=\"8\" style=\"padding:0px; width:1px; height:8px; line-height:8px\" width=\"1\"><img border=\"0\" data-imagetype=\"External\" height=\"8\" src=\"https://simg.grofi.com/2018/ONE/11/SPIN684-CUCO-18897-DE/px.gif\" style=\"display:block; width:1px; height:8px; border:0; visibility:hidden\" width=\"1\" /></td>\n"
            + "                                       </tr>\n"
            + "                                    </tbody>\n"
            + "                                 </table>\n"
            + "                              </td>\n"
            + "                           </tr>\n"
            + "                           <tr valign=\"top\">\n"
            + "                              <td align=\"left\" bgcolor=\"#f8f8f8\" style=\"padding:0px; background-color:#f8f8f8\">\n"
            + "                                 <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse\">\n"
            + "                                    <tbody>\n"
            + "                                       <tr>\n"
            + "                                          <td height=\"25\" style=\"padding:0px; width:1px; height:25px; line-height:25px\" width=\"1\"><img alt=\"\" border=\"0\" data-imagetype=\"External\" height=\"25\" src=\"https://simg.grofi.com/2018/ONE/11/SPIN684-CUCO-18897-DE/px.gif\" style=\"display:block; width:1px; height:25px; border:0; visibility:hidden\" width=\"1\" /></td>\n"
            + "                                       </tr>\n"
            + "                                    </tbody>\n"
            + "                                 </table>\n"
            + "                                 <div style=\"width:640px\">\n"
            + "                                    <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse; width:100%; border-spacing:0\" width=\"100%\">\n"
            + "                                       <tbody>\n"
            + "                                          <tr valign=\"top\">\n"
            + "                                             <td style=\"padding:0px; min-width:25px; width:25px; line-height:1px; font-size:0px\" width=\"25\"><img alt=\"\" border=\"0\" data-imagetype=\"External\" height=\"1\" src=\"https://simg.grofi.com/2018/ONE/11/SPIN684-CUCO-18897-DE/px.gif\" style=\"display:block; width:25px; height:1px; border:0; visibility:hidden\" width=\"25\" /></td>\n"
            + "                                             <td align=\"left\" style=\"padding:0px; min-width:100%; width:100%\" width=\"100%\">\n"
            + "                                                <div style=\"min-height: 140px;\">\n"
            + "                                                   <p><b style=\"font-weight: 600;\">Sehr geehrte/r jane@foobar.com,</b></p>\n"
            + "                                                   <p>Ihr Postfach zum Archiv Ihrer E-Mail wurde erfolgreich angelegt.</p>\n"
            + "                                                   <p>Ihr Login ist Ihre E-Mail-Adresse und Ihr tempor\u00e4res Passwort ist supergeheim. Bitte \u00e4nderen Sie das Passwort, sobald Sie sich eingeloggt haben.</p>\n"
            + "                                                   <p>Verwenden Sie dieses <a href=\"https://e-mail-archiv.grofi.de/deu/dashboard\">Link</a>, um auf Ihr Archiv-Postfach zuzugreifen./p>\n"
            + "                                                </div>\n"
            + "                                                <br />\n"
            + "                                                <hr />\n"
            + "                                                <br />\n"
            + "                                                <p>F\u00fcr R\u00fcckfragen kontaktieren Sie uns bitte unter <a href=\"mailto:support@grofi.de\">support@grofi.de</a> oder telefonisch unter 0721 170 55 22.</p>\n"
            + "                                                <p>Ihr GroFi Team</p>\n"
            + "                                             </td>\n"
            + "                                             <td style=\"padding:0px; min-width:25px; width:25px; line-height:1px; font-size:0px\" width=\"25\"><img alt=\"\" border=\"0\" data-imagetype=\"External\" height=\"1\" src=\"https://simg.grofi.com/2018/ONE/11/SPIN684-CUCO-18897-DE/px.gif\" style=\"display:block; width:25px; height:1px; border:0; visibility:hidden\" width=\"25\" /></td>\n"
            + "                                          </tr>\n"
            + "                                       </tbody>\n"
            + "                                    </table>\n"
            + "                                 </div>\n"
            + "                              </td>\n"
            + "                           </tr>\n"
            + "                           <tr valign=\"top\">\n"
            + "                              <td align=\"left\" bgcolor=\"#f8f8f8\" style=\"padding:0px; background-color:#f8f8f8\">\n"
            + "                                 <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse; width:100%; border-spacing:0\" width=\"100%\">\n"
            + "                                    <tbody>\n"
            + "                                       <tr valign=\"top\">\n"
            + "                                          <td style=\"padding:0px; min-width:25px; width:25px; line-height:1px; font-size:0px\" width=\"25\"><img alt=\"\" border=\"0\" data-imagetype=\"External\" height=\"1\" src=\"https://simg.grofi.com/2018/ONE/11/SPIN684-CUCO-18897-DE/px.gif\" style=\"display:block; width:25px; height:1px; border:0; visibility:hidden\" width=\"25\" /></td>\n"
            + "                                          <td align=\"left\" style=\"padding:0px\">\n"
            + "                                             <p style=\"padding:0 0 0 0; margin:0; text-align:left; margin-bottom:8px; line-height:17px\">  <br /></p>\n"
            + "                                             <p style=\"padding:0; margin:0; text-align:left; margin-bottom:13px; line-height:13px\"><span style=\"font-family:&#39;Overpass Regular&#39;,Arial,sans-serif; font-size:13px; font-weight:normal; color:#636d80; line-height:17px\">--<br><br>GroFi SE<br>Elgendorfer Stra\u00dfe 57<br>56410 Montabaur<br><br>Hauptsitz Montabaur, Amtsgericht Montabaur, HRB 24498<br><br>Vorstand: H\u00fcseyin Dogan, Dr. Martin Endre\u00df, Claudia Frese, Henning Kettler, Arthur Mai, Matthias Steinberg, Achim Wei\u00df</span></p>\n"
            + "                                          </td>\n"
            + "                                          <td style=\"padding:0px; min-width:25px; width:25px; line-height:1px; font-size:0px\" width=\"25\"><img alt=\"\" border=\"0\" data-imagetype=\"External\" height=\"1\" src=\"https://simg.grofi.com/2018/ONE/11/SPIN684-CUCO-18897-DE/px.gif\" style=\"display:block; width:25px; height:1px; border:0; visibility:hidden\" width=\"25\" /></td>\n"
            + "                                       </tr>\n"
            + "                                    </tbody>\n"
            + "                                 </table>\n"
            + "                              </td>\n"
            + "                           </tr>\n"
            + "                        </tbody>\n"
            + "                     </table>\n"
            + "                  </td>\n"
            + "                  <td align=\"left\" style=\"padding:0px; min-width:50%; width:50%\" width=\"50%\">\n"
            + "                     <table bgcolor=\"#003d8f\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse; background-color:#003d8f; width:100%; border-spacing:0\" width=\"100%\">\n"
            + "                        <tbody>\n"
            + "                           <tr>\n"
            + "                              <td height=\"220\" style=\"padding:0px; line-height:220px; height:220px\"><img alt=\"\" border=\"0\" data-imagetype=\"External\" height=\"220\" src=\"https://simg.grofi.com/2018/ONE/11/SPIN684-CUCO-18897-DE/px.gif\" style=\"display:block; width:1px; height:220px; border:0; visibility:hidden\" width=\"1\" /></td>\n"
            + "                           </tr>\n"
            + "                        </tbody>\n"
            + "                     </table>\n"
            + "                  </td>\n"
            + "               </tr>\n"
            + "            </tbody>\n"
            + "         </table>\n"
            + "      </table>\n"
            + "   </body>\n"
            + "</html>";

        String plainText = getHtmlService().html2text(content, true);

        assertTrue("Unexpected plain text content", plainText.indexOf("Verwenden Sie dieses Link https://e-mail-archiv.grofi.de/deu/dashboard, um auf Ihr Archiv-Postfach zuzugreifen.") > 0);
    }

}