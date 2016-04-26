package com.android.gscaparrotti.bendermobile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import model.Dish;
import model.IDish;
import model.Order;
import model.Pair;

public class TableFragment extends Fragment {

    private static final String TABLE_NUMBER = "TABLENMBR";
    private int tableNumber;
    private List<Order> list = new LinkedList<>();
    private DishAdapter adapter;

    private OnTableFragmentInteractionListener mListener;

    public TableFragment() {
    }

    public static TableFragment newInstance(int param1) {
        TableFragment fragment = new TableFragment();
        Bundle args = new Bundle();
        args.putInt(TABLE_NUMBER, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tableNumber = getArguments().getInt(TABLE_NUMBER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_table, container, false);
        TextView text = (TextView) view.findViewById(R.id.tableTitle);
        text.setText(text.getText() + " " + Integer.toString(tableNumber));
        ListView listView = (ListView) view.findViewById(R.id.dishesList);
        adapter = new DishAdapter(getActivity(), list);
        listView.setAdapter(adapter);
        Button update = (Button) view.findViewById(R.id.updateButton);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DownloadDataTask().execute();
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnTableFragmentInteractionListener) {
            mListener = (OnTableFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMainFragmentInteractionListener");
        }
    }

    public void aggiorna(final List<Order> newList) {
        if (list != null) {
            list.clear();
            list.addAll(newList);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
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
    public interface OnTableFragmentInteractionListener {
        void onLoadingInProgress();
    }

    private class DishAdapter extends ArrayAdapter<Order> {

        private LayoutInflater inflater;

        public DishAdapter(Context context, List<Order> persone) {
            super(context, 0, persone);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = inflater.inflate(R.layout.item_dish, parent, false);

            Order order = getItem(position);
            ((TextView) convertView.findViewById(R.id.dish)).setText(order.getDish().getName());
            ((TextView) convertView.findViewById(R.id.dishPrice))
                    .setText("Prezzo Unitario: " + Double.toString(order.getDish().getPrice()));
            ((TextView) convertView.findViewById(R.id.dishToServe))
                    .setText("Piatti da servire: " + Integer.toString(order.getAmounts().getX()));
            ((TextView) convertView.findViewById(R.id.dishServed))
                    .setText("Piatti serviti: " + Integer.toString(order.getAmounts().getY()));
            return convertView;
        }
    }

    private class DownloadDataTask extends AsyncTask<Void, Void, List<Order>> {

        private HttpURLConnection conn;
        private BufferedReader reader;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mListener.onLoadingInProgress();
        }

        @Override
        protected List<Order> doInBackground(Void... params) {
            //qui effettuerò la chiamata al server
            List<Order> temp = new LinkedList<>();
            try {
                final Socket socket = new Socket("10.0.2.2", 6789);
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();
                final DataOutputStream output = new DataOutputStream(os);
                output.writeBytes("GET TABLE " + TableFragment.this.tableNumber + "\n");
                final ObjectInputStream input = new ObjectInputStream(is);
                Map<IDish, Pair<Integer, Integer>> datas = (Map<IDish, Pair<Integer, Integer>>) input.readObject();
                temp = new LinkedList<>();
                for(Map.Entry<IDish, Pair<Integer, Integer>> entry : datas.entrySet()) {
                    temp.add(new Order(entry.getKey(), entry.getValue()));
                }
                socket.close();
            } catch (IOException  | ClassNotFoundException | ClassCastException e) {
                Log.d("exception", e.getMessage());
                temp = new LinkedList<>();
                temp.add(new Order(new Dish(e.getMessage(), 0), new Pair<>(0, 0)));
            }
            return temp;
        }

        @Override
        protected void onPostExecute(List<Order> orders) {
            super.onPostExecute(orders);
            TableFragment.this.aggiorna(orders);
        }
    }

}
