package mx.com.redcup.redcup;


import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.location.Geofence;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import io.nlopez.smartlocation.OnGeofencingTransitionListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.geofencing.model.GeofenceModel;
import io.nlopez.smartlocation.geofencing.utils.TransitionGeofence;
import mx.com.redcup.redcup.Holders_extensions.BottomSheetInviteFriends;
import mx.com.redcup.redcup.Holders_extensions.CommentsRecyclerHolder;
import mx.com.redcup.redcup.myDataModels.InviteStatus;
import mx.com.redcup.redcup.myDataModels.MyEventComments;
import mx.com.redcup.redcup.myDataModels.MyEvents;
import mx.com.redcup.redcup.myDataModels.MyUsers;


public class EventDetailsActivity extends AppCompatActivity implements OnGeofencingTransitionListener {

    private static final String TAG = "EventDetailsActivity" ;
    public DatabaseReference mDatabase_events = FirebaseDatabase.getInstance().getReference().child("Events_parent");
    public DatabaseReference mDatabase_users = FirebaseDatabase.getInstance().getReference().child("Users_parent");
    public StorageReference mStorage = FirebaseStorage.getInstance().getReference();

    TextView postContent;
    TextView authorName;
    ImageView authorPic;
    TextView displayTime;
    Button faveEvent;
    Button shareEvent;
    Button inviteFriend;
    Button menuMore;
    RecyclerView commentsRecyclerView;
    EditText commentInputField;
    Button commentSendButton;
    CollapsingToolbarLayout toolbarTitle;
    FloatingActionMenu floatingMenu;
    com.github.clans.fab.FloatingActionButton fabConfirmAttendance;
    com.github.clans.fab.FloatingActionButton fabDeclineAttendance;
    com.github.clans.fab.FloatingActionButton fabMaybeAttendance;

    public String authorUserID;
    public Double currentEventLat;
    public Double currentEventLng;
    public Boolean isEventLiked;

    public RecyclerView.Adapter commentAdapter;

    View.OnClickListener openProfileDetails = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(),ProfileDetailsActivity.class);
            intent.putExtra("user_id",authorUserID);
            startActivity(intent);
        }
    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        //Get the extra info passed on by previous activity. i.e the event id, so firebase data can be retrieved.
        Intent intent = getIntent();
        final String postID = intent.getStringExtra("event_id");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Get handle of UI elements
        postContent = (TextView) findViewById(R.id.tv_EventDetails_event_contentn);
        toolbarTitle = (CollapsingToolbarLayout) findViewById(R.id.ct_eventdetails_title);
        floatingMenu = (FloatingActionMenu) findViewById(R.id.fam_attendance_status);
        fabConfirmAttendance = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fab_confirm);
        fabDeclineAttendance = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fab_decline);
        fabMaybeAttendance = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fab_maybe);
        authorName = (TextView) findViewById(R.id.tv_eventDetails_userName);
        authorPic = (ImageView) findViewById(R.id.tv_eventdetails_userpic);
        displayTime = (TextView) findViewById(R.id.tv_display_time);
        faveEvent = (Button) findViewById(R.id.btn_eventdetails_fav);
        shareEvent = (Button) findViewById(R.id.btn_eventdetails_share);
        inviteFriend = (Button) findViewById(R.id.btn_eventdetails_invite);
        menuMore = (Button) findViewById(R.id.btn_eventdetails_more);
        commentInputField = (EditText) findViewById(R.id.et_eventdetails_commentfield);
        commentSendButton = (Button) findViewById(R.id.btn_eventdetails_sendcomment);
        commentsRecyclerView = (RecyclerView) findViewById(R.id.rv_eventdetails_comments);


        //Set onClick listeners for the FABs to log the user as whatever status they chose.
        //TODO Add a function to write new post to their profiles saying they changed their attendace status to <insert status>
        fabConfirmAttendance.setOnClickListener(new View.OnClickListener() { //User checked as attending
            @Override
            public void onClick(View view) {confirmUserAttendance(postID,view);
            }
        });
        fabDeclineAttendance.setOnClickListener(new View.OnClickListener() {//User checked as not attending
            @Override
            public void onClick(View view) {declineInvitation(postID,view);
            }
        });
        fabMaybeAttendance.setOnClickListener(new View.OnClickListener() {//User checked as not sure if attending
            @Override
            public void onClick(View view) {markAttendanceUncertain(postID,view);
            }
        });
        faveEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                faveThisEvent(postID,v);
            }
        });
        shareEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareThisEvent(postID,v);
            }
        });
        inviteFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inviteFriend(postID,v);
            }
        });
        menuMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMoreMenu(postID,v);
            }
        });
        commentSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = commentInputField.getText().toString();
                postNewComment(postID,content,v);
            }
        });
        commentInputField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {floatingMenu.setVisibility(View.GONE);
            }
        });


        authorPic.setOnClickListener(openProfileDetails);

        //Fill the fields for event name, profile picture, etc
        populateActivityData(postID);

        commentsRecyclerView.setHasFixedSize(false);

        final DatabaseReference comments_ref = mDatabase_events.child(postID).child("event_comments");
        commentAdapter = new FirebaseRecyclerAdapter<MyEventComments,CommentsRecyclerHolder>(MyEventComments.class,
                R.layout.recyclerrow_comments, CommentsRecyclerHolder.class,comments_ref ) {
            @Override
            protected void populateViewHolder(CommentsRecyclerHolder viewHolder, MyEventComments comment, int position) {
                viewHolder.setParentEventID(comment.getParentEventID());
                viewHolder.setCommentID(comment.getCommentID());
                viewHolder.setContent(comment.getCommentContent());
                viewHolder.setAuthorPic(comment.getAuthorUID());

            }
        };
        commentsRecyclerView.setAdapter(commentAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        commentsRecyclerView.setLayoutManager(llm);


    }

    public void confirmUserAttendance(String postID,View view){
        //Get the postId, and add the userID to the list of attendees
        DatabaseReference attendance_listRef = mDatabase_events.child(postID).child("invitee_list");
        String userUid = getCurrentFirebaseUID();

        Map<String, Object> inviteUpdate = new HashMap<>();
        inviteUpdate.put(userUid, InviteStatus.INVITE_ACCEPTED);

        attendance_listRef.updateChildren(inviteUpdate);

        activateGeofence(postID);

        Snackbar.make(view,"You marked as attending",Snackbar.LENGTH_SHORT).show();
    }

    public void declineInvitation(String postID, View view){
        DatabaseReference attendance_listRef = mDatabase_events.child(postID).child("invitee_list");
        String userUid = getCurrentFirebaseUID();
        Map<String, Object> attendanceUpdate = new HashMap<>();
        attendanceUpdate.put(userUid, InviteStatus.INVITE_DECLINED);

        attendance_listRef.updateChildren(attendanceUpdate);

        Snackbar.make(view,"You declined your invitation to this event",Snackbar.LENGTH_SHORT).show();
    }

    public void markAttendanceUncertain(String postID, View view){
        DatabaseReference attendance_listRef = mDatabase_events.child(postID).child("invitee_list");
        String userUid = getCurrentFirebaseUID();

        Map<String, Object> attendanceUpdate = new HashMap<>();
        attendanceUpdate.put(userUid,InviteStatus.INVITE_UNCERTAIN);

        attendance_listRef.updateChildren(attendanceUpdate);

        Snackbar.make(view,"You marked your attendance as uncertain",Snackbar.LENGTH_SHORT).show();
    }

    public void faveThisEvent(String postID, View view){
        String currentUID = getCurrentFirebaseUID();
        DatabaseReference user1_friendsRef = mDatabase_events.child(postID).child("likes"); //Author ref

        if ( !isEventLiked) {//Liked for the first time
            //Marking the user liked the post
            Map<String, Object> newLike = new HashMap<>();
            newLike.put(currentUID, currentUID);

            user1_friendsRef.updateChildren(newLike);
            faveEvent.setBackground(getResources().getDrawable(R.drawable.ic_favorite_red));
            isEventLiked = true;
        } else {
            user1_friendsRef.child(currentUID).removeValue();
            faveEvent.setBackground(getResources().getDrawable(R.drawable.ic_favorite_black_24dp));
            isEventLiked = false;
        }
    }

    public void shareThisEvent(String postID, View view){
        //TODO: Implement dynamic links, and then prompt a modal bottom fragment to share;
        Intent intent = new AppInviteInvitation.IntentBuilder("title")
                .setMessage("title")
                .setDeepLink(Uri.parse("https://cb8v7.app.goo.gl/"))
                .build();


        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");
        sendIntent.setType("text/plain");
        startActivity(intent);
    }

    public void inviteFriend(String postID, View view){
        //TODO: Promopt bottom modal fragment and show autocomplete list of user's friends, then send a notification to invitee
        Snackbar.make(view, "This is still in development...",Snackbar.LENGTH_SHORT).show();

        String currentUID = getCurrentFirebaseUID();
        Bundle extras = new Bundle();
        extras.putString("userID",currentUID);

        BottomSheetInviteFriends dialogFragment = new BottomSheetInviteFriends();
        dialogFragment.setArguments(extras);

        dialogFragment.show(getSupportFragmentManager(), "Tag");
    }

    public void openMoreMenu(final String postID, final View view){

        final Dialog extraEventOptions = new Dialog(this);
        extraEventOptions.setContentView(R.layout.dialog_event_options);

        Button editEvent = (Button) extraEventOptions.findViewById(R.id.btn_eventdialog_edit);
        Button deleteEvent = (Button) extraEventOptions.findViewById(R.id.btn_eventdialog_delete);
        Button addAuthorAsFriend = (Button) extraEventOptions.findViewById(R.id.btn_eventdialog_addFriend);

        if (authorUserID.equals(getCurrentFirebaseUID())){
            addAuthorAsFriend.setVisibility(View.GONE);
        }else {
            editEvent.setEnabled(false);
            deleteEvent.setEnabled(false);
        }

        extraEventOptions.show();

        editEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(v,"not yet functional",Toast.LENGTH_SHORT).show();
                extraEventOptions.dismiss();
            }
        });

        deleteEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatabase_events.child(postID).removeValue();
                mDatabase_users.child(authorUserID).child("user_posts").child(postID).removeValue();
                extraEventOptions.dismiss();
            }
        });

        addAuthorAsFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentUID = getCurrentFirebaseUID();
                DatabaseReference user1_friendsRef = mDatabase_users.child(authorUserID).child("userFriends"); //Author ref
                DatabaseReference user2_friendsRef = mDatabase_users.child(currentUID).child("userFriends"); // Viewer ref

                //For author
                Map<String, Object> userFiend = new HashMap<>();
                userFiend.put(currentUID,currentUID);

                //For viewer
                Map<String, Object> newFriend = new HashMap<>();
                newFriend.put(authorUserID,authorUserID);

                user1_friendsRef.updateChildren(userFiend);
                user2_friendsRef.updateChildren(newFriend);

                Snackbar.make(view, "You just made a new friend!", Snackbar.LENGTH_SHORT).show();
                extraEventOptions.dismiss();
            }
        });
    }

    public void postNewComment(String postID, String commentContent, View view){
        if (TextUtils.isEmpty(commentContent)){
            commentInputField.setError("Required");
        } else{
            DatabaseReference comments_Ref = mDatabase_events.child(postID).child("event_comments");
            String pushID = comments_Ref.push().getKey();

            MyEventComments newComment = new MyEventComments(commentContent,getCurrentFirebaseUID(),pushID,postID);

            Map<String, Object> commentUpdate = new HashMap<>();
            commentUpdate.put(pushID, newComment);

            comments_Ref.updateChildren(commentUpdate);
            commentInputField.setText(null);
            floatingMenu.setVisibility(View.VISIBLE);
            Snackbar.make(view,"Comment posted",Snackbar.LENGTH_SHORT).show();
        }
    }

    public void activateGeofence(String postID){
        Toast.makeText(this,"The geofence was activated", Toast.LENGTH_SHORT).show();
        //TODO: Activate geofence
        GeofenceModel eventGeofence = new GeofenceModel.Builder(postID)
                .setTransition(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setLatitude(currentEventLat).setLongitude(currentEventLng)
                .setRadius(150).build();

        SmartLocation.with(getApplicationContext()).geofencing().add(eventGeofence)
                .start(this);
    }

    public void setUserData(String uID){
        Glide.with(getApplicationContext()).using(new FirebaseImageLoader())
                .load(mStorage.child(uID).child("profile_picture")).signature(new StringSignature(String.valueOf(System.currentTimeMillis())))
                .into(authorPic);

        mDatabase_users.child(uID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MyUsers user = dataSnapshot.getValue(MyUsers.class);
                authorName.setText(user.getDisplayName());

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public void populateActivityData(String postID){
        DatabaseReference queryPoint = mDatabase_events.child(postID);
        queryPoint.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MyEvents event = dataSnapshot.getValue(MyEvents.class);

                authorUserID = event.getUserID();
                currentEventLat = event.getEventLatitude();
                currentEventLng = event.getEventLongitude();

                postContent.setText(event.getEventContent());
                toolbarTitle.setTitle(event.getEventName());
                authorName.setText(event.getUserID());
                setUserData(event.getUserID());
                String time = (event.getEventHour() + ":" + event.getEventMinutes() + " pm" );
                displayTime.setText(time);

                if (dataSnapshot.child("likes").child(getCurrentFirebaseUID()).exists()){
                    faveEvent.setBackground(getResources().getDrawable(R.drawable.ic_favorite_red));
                    isEventLiked = true;
                }else {
                    faveEvent.setBackground(getResources().getDrawable(R.drawable.ic_favorite_black_24dp));
                    isEventLiked = false;
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void onGeofenceTransition(TransitionGeofence transitionGeofence) {

        Toast.makeText(this, "You just entered a new event", Toast.LENGTH_SHORT).show();
    }

    public String getCurrentFirebaseUID(){
        String UID = "";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            UID = user.getUid();
        } else {
            Log.e(TAG,"User is unexpectedly null.");
        }
        return UID;
    }
}
