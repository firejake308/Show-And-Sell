package com.insertcoolnamehere.showandsell;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnDonationListener} interface
 * to handle interaction events.
 * Use the {@link DonateFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DonateFragment extends Fragment {

    private OnDonationListener mListener;

    private Spinner conditionEntry;
    private Button takePicBtn;

    public DonateFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DonateFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DonateFragment newInstance(String param1, String param2) {
        DonateFragment fragment = new DonateFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_donate, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // populate condition spinner
        conditionEntry = (Spinner) getActivity().findViewById(R.id.item_condition_entry);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.possible_conditions, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        conditionEntry.setAdapter(adapter);

        // hook up action listeners
        takePicBtn = (Button) getActivity().findViewById(R.id.upload_img_btn);
        takePicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptTakePic();
            }
        });

        Button donateBtn = (Button) getActivity().findViewById(R.id.donate_btn);
        donateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDonation();
            }
        });
    }

    // TODO: Rename method and hook method into UI event
    public void attemptTakePic() {
        if (mListener != null) {
            mListener.takePic();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDonationListener) {
            mListener = (OnDonationListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDonationListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnDonationListener {
        void takePic();
        void onDonation();
    }
}
