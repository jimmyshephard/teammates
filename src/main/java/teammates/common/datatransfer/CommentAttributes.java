package teammates.common.datatransfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import teammates.common.util.FieldValidator;
import teammates.common.util.Sanitizer;
import teammates.common.util.FieldValidator.FieldType;
import teammates.storage.entity.Comment;

import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.ScoredDocument;

public class CommentAttributes extends EntityAttributes 
    implements Comparable<CommentAttributes>{

    private Long commentId = null;
    public String courseId;
    public String giverEmail;
    public CommentRecipientType recipientType = CommentRecipientType.PERSON;
    public Set<String> recipients;
    public CommentStatus status = CommentStatus.FINAL;
    public CommentSendingState sendingState = CommentSendingState.SENT;
    public List<CommentRecipientType> showCommentTo;
    public List<CommentRecipientType> showGiverNameTo;
    public List<CommentRecipientType> showRecipientNameTo;
    public Text commentText;
    public Date createdAt;

    public CommentAttributes() {

    }

    public CommentAttributes(String courseId, String giverEmail, CommentRecipientType recipientType,
            Set<String> recipients, Date createdAt, Text commentText) {
        this.courseId = courseId;
        this.giverEmail = giverEmail;
        this.recipientType = recipientType != null ? recipientType : CommentRecipientType.PERSON;
        this.recipients = recipients;
        this.commentText = commentText;
        this.createdAt = createdAt;
    }

    public CommentAttributes(Comment comment) {
        this.commentId = comment.getId();
        this.courseId = comment.getCourseId();
        this.giverEmail = comment.getGiverEmail();
        this.recipientType = comment.getRecipientType();
        this.status = comment.getStatus();
        this.sendingState = comment.getSendingState() != null? comment.getSendingState() : CommentSendingState.SENT;
        this.showCommentTo = comment.getShowCommentTo();
        this.showGiverNameTo = comment.getShowGiverNameTo();
        this.showRecipientNameTo = comment.getShowRecipientNameTo();
        this.recipients = comment.getRecipients();
        this.createdAt = comment.getCreatedAt();
        this.commentText = comment.getCommentText();
    }

    public Long getCommentId() {
        return this.commentId;
    }

    // Use only to match existing and known Comment
    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public List<String> getInvalidityInfo() {

        FieldValidator validator = new FieldValidator();
        List<String> errors = new ArrayList<String>();
        String error;

        error = validator.getInvalidityInfo(FieldType.COURSE_ID, courseId);
        if (!error.isEmpty()) {
            errors.add(error);
        }

        error = validator.getInvalidityInfo(FieldType.EMAIL, giverEmail);
        if (!error.isEmpty()) {
            errors.add(error);
        }

        if (recipientType != null) {
            switch (recipientType) {
            case PERSON:
                for (String recipientId : recipients) {
                    error = validator.getInvalidityInfo(FieldType.EMAIL, recipientId);
                    if (!error.isEmpty()) {
                        errors.add(error);
                    }
                }
                break;
            case TEAM:
                for (String recipientId : recipients) {
                    error = validator.getInvalidityInfo(FieldType.TEAM_NAME, recipientId);
                    if (!error.isEmpty()) {
                        errors.add(error);
                    }
                }
                break;
            case SECTION:
                // TODO: implement this
                break;
            case COURSE:
                for (String recipientId : recipients) {
                    error = validator.getInvalidityInfo(FieldType.COURSE_ID, recipientId);
                    if (!error.isEmpty()) {
                        errors.add(error);
                    }
                }
                break;
            default:// cases for NONE or null
                break;
            }
        }

        return errors;
    }

    public Comment toEntity() {
        return new Comment(courseId, giverEmail, recipientType, recipients, status,
                sendingState,
                showCommentTo, 
                showGiverNameTo, 
                showRecipientNameTo, 
                commentText, createdAt);
    }
    
    public Boolean isVisibleTo(CommentRecipientType targetViewer){
        return showCommentTo.contains(targetViewer);
    }

    @Override
    public String toString() {
        return "CommentAttributes [commentId = " + commentId +
                ", courseId = " + courseId +
                ", giverEmail = " + giverEmail +
                ", recipientType = " + recipientType +
                ", recipient = " + recipients +
                ", status = " + status +
                ", showCommentTo = " + showCommentTo +
                ", showGiverNameTo = " + showGiverNameTo +
                ", showRecipientNameTo = " + showRecipientNameTo +
                ", commentText = " + commentText +
                ", createdAt = " + createdAt + "]";
    }

    @Override
    public String getIdentificationString() {
        return toString();
    }

    @Override
    public String getEntityTypeAsString() {
        return "Comment";
    }

    @Override
    public void sanitizeForSaving() {
        this.courseId = this.courseId.trim();
        this.commentText = Sanitizer.sanitizeTextField(this.commentText);
        this.courseId = Sanitizer.sanitizeForHtml(courseId);
        this.giverEmail = Sanitizer.sanitizeForHtml(giverEmail);

        HashSet<String> sanitizedRecipients = new HashSet<String>();
        for (String recipientId : recipients) {
            sanitizedRecipients.add(Sanitizer.sanitizeForHtml(recipientId));
        }
        recipients = sanitizedRecipients;

        if (commentText != null) {
            this.commentText = new Text(Sanitizer.sanitizeForHtml(commentText.getValue()));
        }
        
        sanitizeForVisibilityOptions();
    }

    private void sanitizeForVisibilityOptions() {
        switch(recipientType){
        case PERSON:
            removeCommentRecipientTypeIn(showRecipientNameTo, CommentRecipientType.PERSON);
            break;
        case TEAM:
            removeCommentRecipientTypeInVisibilityOptions(CommentRecipientType.PERSON);
            removeCommentRecipientTypeIn(showRecipientNameTo, CommentRecipientType.TEAM);
            break;
        case SECTION:
            removeCommentRecipientTypeInVisibilityOptions(CommentRecipientType.PERSON);
            removeCommentRecipientTypeInVisibilityOptions(CommentRecipientType.TEAM);
            removeCommentRecipientTypeIn(showRecipientNameTo, CommentRecipientType.SECTION);
            break;
        case COURSE:
            removeCommentRecipientTypeInVisibilityOptions(CommentRecipientType.PERSON);
            removeCommentRecipientTypeInVisibilityOptions(CommentRecipientType.TEAM);
            removeCommentRecipientTypeInVisibilityOptions(CommentRecipientType.SECTION);
            removeCommentRecipientTypeIn(showRecipientNameTo, CommentRecipientType.COURSE);
            break;
        default:
            break;
        }
    }
    
    private void removeCommentRecipientTypeInVisibilityOptions(CommentRecipientType typeToRemove){
        removeCommentRecipientTypeIn(showCommentTo, typeToRemove);
        removeCommentRecipientTypeIn(showGiverNameTo, typeToRemove);
        removeCommentRecipientTypeIn(showRecipientNameTo, typeToRemove);
    }
    
    private void removeCommentRecipientTypeIn(List<CommentRecipientType> visibilityOptions, 
            CommentRecipientType typeToRemove){
        if(visibilityOptions == null) return;
        
        Iterator<CommentRecipientType> iter = visibilityOptions.iterator();
        while(iter.hasNext()){
            CommentRecipientType otherType = iter.next();
            if(otherType == typeToRemove){
                iter.remove();
            }
        }
    }

    public static void sortCommentsByCreationTime(List<CommentAttributes> comments) {
        Collections.sort(comments, new Comparator<CommentAttributes>() {
            public int compare(CommentAttributes comment1, CommentAttributes comment2) {
                return comment1.createdAt.compareTo(comment2.createdAt);
            }
        });
    }

    public static void sortCommentsByCreationTimeDescending(List<CommentAttributes> comments) {
        Collections.sort(comments, new Comparator<CommentAttributes>() {
            public int compare(CommentAttributes comment1, CommentAttributes comment2) {
                return comment2.createdAt.compareTo(comment1.createdAt);
            }
        });
    }
    
    @Override
    public int compareTo(CommentAttributes o) {
        if(o == null){
            return 1;
        }
        return o.createdAt.compareTo(createdAt);
    }
    
    public Document toDocument(CourseAttributes course, InstructorAttributes instructor, Map<String, StudentAttributes> emailStudentTable){
        StringBuilder recipientEmailsBuilder = new StringBuilder("");
        StringBuilder recipientNamesBuilder = new StringBuilder("");
        StringBuilder recipientTeamsBuilder = new StringBuilder("");
        StringBuilder recipientSectionsBuilder = new StringBuilder("");
        String delim = "";
        switch (this.recipientType) {
        case PERSON:
            for(String email:this.recipients){
                StudentAttributes student = emailStudentTable.get(email);
                recipientEmailsBuilder.append(delim).append(email); 
                if(student != null){
                    recipientNamesBuilder.append(delim).append(student.name);
                    recipientTeamsBuilder.append(delim).append(student.team);
                    recipientSectionsBuilder.append(delim).append(student.section);
                }
                delim = ",";
            }
            
            break;
        case TEAM:
            for(String team:this.recipients){
                recipientTeamsBuilder.append(delim).append(team); 
                delim = ",";
            }
            break;
        case SECTION:
            for(String section:this.recipients){
                recipientSectionsBuilder.append(delim).append(section); 
                delim = ",";
            }
            break;
        default:
            break;
        }
        Document doc = Document.newBuilder().addField(Field.newBuilder().setName("type").setText("comment"))
            .addField(Field.newBuilder().setName("courseId").setText(this.courseId))
            .addField(Field.newBuilder().setName("courseName").setText(course != null? course.name: ""))
            .addField(Field.newBuilder().setName("giverEmail").setText(this.giverEmail))
            .addField(Field.newBuilder().setName("giverName").setText(instructor != null? instructor.name: this.giverEmail))
            .addField(Field.newBuilder().setName("giverTitle").setText(instructor != null? instructor.displayedName: ""))
            .addField(Field.newBuilder().setName("recipientType").setText(this.recipientType.toString()))
            .addField(Field.newBuilder().setName("recipientEmails").setText(recipientEmailsBuilder.toString()))
            .addField(Field.newBuilder().setName("recipientNames").setText(recipientNamesBuilder.toString()))
            .addField(Field.newBuilder().setName("recipientTeams").setText(recipientTeamsBuilder.toString()))
            .addField(Field.newBuilder().setName("recipientSections").setText(recipientSectionsBuilder.toString()))
            .addField(Field.newBuilder().setName("status").setText(this.status.toString()))
            .addField(Field.newBuilder().setName("sendingState").setText(this.sendingState.toString()))
            .addField(Field.newBuilder().setName("showCommentTo").setText(this.showCommentTo.toString()))
            .addField(Field.newBuilder().setName("showGiverNameTo").setText(this.showGiverNameTo.toString()))
            .addField(Field.newBuilder().setName("showRecipientNameTo").setText(this.showRecipientNameTo.toString()))
            .addField(Field.newBuilder().setName("createdAt").setDate(this.createdAt))
            .addField(Field.newBuilder().setName("commentText").setText(this.commentText.getValue()))
            .setId(this.commentId.toString())
            .build();
        return doc;
    }
    
    public static CommentAttributes fromDocument(ScoredDocument doc){
        CommentAttributes comment = new CommentAttributes();
        comment.commentId = Long.valueOf(doc.getId());
        comment.courseId = doc.getOnlyField("courseId").getText();
        comment.giverEmail = doc.getOnlyField("giverEmail").getText();
        
        comment.recipientType = CommentRecipientType.valueOf(doc.getOnlyField("recipientType").getText());
        String[] recipients = null;
        if(comment.recipientType == CommentRecipientType.PERSON){
            recipients = doc.getOnlyField("recipientEmails").getText().split(",");
        } else if(comment.recipientType == CommentRecipientType.TEAM){
            recipients = doc.getOnlyField("recipientTeams").getText().split(",");
        } else if(comment.recipientType == CommentRecipientType.SECTION){
            recipients = doc.getOnlyField("recipientSections").getText().split(",");
        } else if(comment.recipientType == CommentRecipientType.COURSE){
            recipients = new String[]{""};
        }
        comment.recipients = new HashSet<String>();
        for(String recipient:recipients){
            comment.recipients.add(recipient);
        }
        //TODO: get status/visibility options etc
        comment.createdAt = doc.getOnlyField("createdAt").getDate();
        comment.commentText = new Text(doc.getOnlyField("commentText").getText());
        return comment;
    }
    
}
