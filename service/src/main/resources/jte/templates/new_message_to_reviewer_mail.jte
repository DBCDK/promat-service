@import dk.dbc.promat.service.templating.model.MailToReviewerOnNewMessage
@param MailToReviewerOnNewMessage mailToReviewerOnNewMessage

!{var messageText=mailToReviewerOnNewMessage.getMessage().getMessageText();}
!{var promatCase=mailToReviewerOnNewMessage.getPromatCase();}



<p>Kære ${promatCase.getReviewer().getFirstName()} ${promatCase.getReviewer().getLastName()}</p>
<p>Der er en ny besked til dig fra redaktøren:</p>
<p style="margin-left: 15px; width: 275px; margin-bottom: 15px; margin-top: 15px;"><i>
    "$unsafe{messageText.replace("\n", "<br/>")}"</i></p>

<p>Med venlig hilsen,<br/>
    Redaktionen
</p>
