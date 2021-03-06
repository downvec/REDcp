package mx.com.redcup.redcup.myNavigationFragments;



import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseIndexRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import mx.com.redcup.redcup.Holders_extensions.EventsRecyclerHolder;
import mx.com.redcup.redcup.NavActivity;
import mx.com.redcup.redcup.R;
import mx.com.redcup.redcup.myDataModels.MyEventComments;
import mx.com.redcup.redcup.myDataModels.MyEvents;
import static android.content.ContentValues.TAG;

public class NearbyFragment extends Fragment {

    public DatabaseReference mDatabase;
    FloatingActionButton createPost;
    RelativeLayout myContainer;
    TextView emptyRVMessage;
    RecyclerView rv;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_nearby, container, false);

        // Getting handle of the Recycler view on the layout
        rv = (RecyclerView) rootView.findViewById(R.id.rv_recycler_view);
        createPost = (FloatingActionButton) rootView.findViewById(R.id.fab_create_post);
        myContainer = (RelativeLayout) rootView.findViewById(R.id.container_newpost);
        emptyRVMessage = (TextView) rootView.findViewById(R.id.emptyRv_message);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setReverseLayout(true);
        llm.setStackFromEnd(true);

        rv.setHasFixedSize(false);

        //TODO: Change the recycleradapter to indexed recycler adapter. Indexes at Db/feeds

        DatabaseReference indexRef = FirebaseDatabase.getInstance().getReference().child("Feeds").child(getCurrentFirebaseUID());
        DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference().child("Events_parent");

        RecyclerView.Adapter feedAdapter = new FirebaseIndexRecyclerAdapter<MyEvents,EventsRecyclerHolder>(
        MyEvents.class, R.layout.recyclerrow_events, EventsRecyclerHolder.class, indexRef,dataRef) {
            @Override
            protected void populateViewHolder(EventsRecyclerHolder viewHolder, MyEvents event, int position) {
                if (this.getItemCount() != 0) {
                    viewHolder.setSponsoredStatus(event.isSponsored());
                    viewHolder.setContent(event.getEventContent());
                    viewHolder.setTitle(event.getEventName());
                    viewHolder.setPostID(event.getEventID());
                    if (event.getContentType().equals("Event")) {
                        viewHolder.setProfilePic(event.getUserID());
                    } else {
                        viewHolder.displayEventImage(event.getEventID());
                        viewHolder.setProfilePic(event.getUserID());
                    }
                }else{
                    setEmptyRVMessage();
                }
            }
        };

        rv.setAdapter(feedAdapter);

        rv.setLayoutManager(llm);

        createPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostFragment map = new PostFragment();

                FragmentTransaction fm = getFragmentManager().beginTransaction().add(R.id.Nav_activity_content, map).addToBackStack("Fragment");
                fm.commit();

            }
        });

        return rootView;
    }

    public void setEmptyRVMessage(){
        rv.setVisibility(View.GONE);
        emptyRVMessage.setVisibility(View.VISIBLE);
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
