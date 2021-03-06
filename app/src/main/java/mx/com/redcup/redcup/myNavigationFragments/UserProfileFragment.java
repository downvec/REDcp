package mx.com.redcup.redcup.myNavigationFragments;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import com.bumptech.glide.signature.StringSignature;
import com.firebase.ui.database.FirebaseIndexRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import mx.com.redcup.redcup.Holders_extensions.UserProfileEventsHolder;
import mx.com.redcup.redcup.R;
import mx.com.redcup.redcup.myDataModels.MyEvents;
import mx.com.redcup.redcup.myDataModels.MyUsers;

import static android.app.Activity.RESULT_OK;


public class UserProfileFragment extends Fragment  {
    String TAG = "UserProfileFragment";
    public Context mcontext;

    MyUsers userProfile;

    //ProfilePictureView profilePictureView;
    CollapsingToolbarLayout toolbarLayout;
    ImageView profilePicture;
    ImageView imageBackground;
    TextView mUserName;


    DatabaseReference mDataBaseRef_users = FirebaseDatabase.getInstance().getReference().child("Users_parent");
    DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    View.OnClickListener coverImagePickerBuilder = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ) {

                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return;
            }
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, 1);
        }
    };

    View.OnClickListener profileImagePickerBuilder = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ) {

                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return;
            }
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, 2);
        }
    };

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_user_profile, container, false);
        mcontext = getActivity();
        final String currentUID = getCurrentFirebaseUID();

        //Getting UI elements
        toolbarLayout = (CollapsingToolbarLayout) view.findViewById(R.id.ct_userProfile_title);
        profilePicture = (ImageView) view.findViewById(R.id.civ_profilePicture);
        imageBackground = (ImageView) view.findViewById(R.id.image_background);
        //profilePictureView = (ProfilePictureView) view.findViewById(R.id.iv_profile_userpic);

        toolbarLayout.setOnClickListener(coverImagePickerBuilder);
        profilePicture.setOnClickListener(profileImagePickerBuilder);

        // Getting handle of the Recycler view on the layout
        RecyclerView rv = (RecyclerView) view.findViewById(R.id.rv_user_profile);
        rv.setHasFixedSize(false);

        //Using Firebase-UI library: FirebaseAdapter to create a recycler view getting data straight from firebase
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Events_parent");
        DatabaseReference mPostRef = FirebaseDatabase.getInstance().getReference().child("Users_parent").child(getCurrentFirebaseUID()).child("user_posts");
        mStorageRef = FirebaseStorage.getInstance().getReference().child(getCurrentFirebaseUID());


        RecyclerView.Adapter indexedAdapter = new FirebaseIndexRecyclerAdapter<MyEvents, UserProfileEventsHolder>(
                MyEvents.class, R.layout.recyclerrow_events, UserProfileEventsHolder.class, mPostRef, mDatabase) {
            @Override
            protected void populateViewHolder(UserProfileEventsHolder viewHolder, MyEvents event, int position) {
                viewHolder.setTitle(event.getEventContent()); //Note: This switching is on purpose. Content and title were mixed somewhere
                viewHolder.setContent(event.getEventName());
                viewHolder.setProfilePic(event.getUserID());
            }
        };

        rv.setAdapter(indexedAdapter);


        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(llm);

        //Firebase querying to set name and profile picture to user values
        populateActivityData();


        return view;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    //End LifeCycle and native methods


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

    public void populateActivityData(){
        mDataBaseRef_users.child(getCurrentFirebaseUID()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userProfile = dataSnapshot.getValue(MyUsers.class);
                //profilePictureView.setProfileId(userProfile.getFacebookUID());
                Glide.with(mcontext).using(new FirebaseImageLoader())
                        .load(mStorageRef.child("profile_picture")).signature(new StringSignature(String.valueOf(System.currentTimeMillis()))).into(profilePicture);
                toolbarLayout.setTitle(userProfile.getDisplayName());
                //toolbarLayout.setContentScrimResource(R.drawable.redcupa);
                Glide.with(mcontext).using(new FirebaseImageLoader())
                        .load(mStorageRef.child("cover_image")).signature(new StringSignature(String.valueOf(System.currentTimeMillis()))).into(imageBackground);
                //imageBackground.setImageResource(R.drawable.redcupa);

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent returnedImage) {
        if (resultCode == RESULT_OK){
            StorageMetadata metadata = new StorageMetadata.Builder().setContentType("image/png").setContentEncoding("png").build();
            if (requestCode == 1) {
                Uri imageUri = returnedImage.getData();
                mStorageRef.child("cover_image").putFile(imageUri,metadata);
                Glide.with(mcontext).using(new FirebaseImageLoader())
                        .load(mStorageRef.child("cover_image")).signature(new StringSignature(String.valueOf(System.currentTimeMillis()))).into(imageBackground);

                //mDataBaseRef_users.child(getCurrentFirebaseUID()).child("cover_image_name").setValue("");
            } else if (requestCode == 2){
                Uri imageUri = returnedImage.getData();
                mStorageRef.child("profile_picture").putFile(imageUri,metadata);
                Glide.with(mcontext).using(new FirebaseImageLoader())
                        .load(mStorageRef.child("profile_picture")).signature(new StringSignature(String.valueOf(System.currentTimeMillis()))).into(profilePicture);

            }

            populateActivityData();

        }
    }
}
