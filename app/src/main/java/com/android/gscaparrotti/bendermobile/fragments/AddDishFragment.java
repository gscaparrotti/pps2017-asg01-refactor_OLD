package com.android.gscaparrotti.bendermobile.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gscaparrotti.bendermobile.R;
import com.android.gscaparrotti.bendermobile.activities.MainActivity;
import com.android.gscaparrotti.bendermobile.network.ServerInteractor;

import java.util.LinkedList;
import java.util.List;

import model.Dish;
import model.IDish;
import model.IMenu;
import model.Order;
import model.OrderedDish;
import model.Pair;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnAddDishFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AddDishFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddDishFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private int tableNumber;
    private List<IDish> list = new LinkedList<>();
    private AddDishAdapter adapter;

    private OnAddDishFragmentInteractionListener mListener;

    public AddDishFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tableNumber Parameter 1.
     * @return A new instance of fragment AddDishFragment.
     */
    public static AddDishFragment newInstance(int tableNumber) {
        AddDishFragment fragment = new AddDishFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, tableNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tableNumber = getArguments().getInt(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_dish, container, false);
        ListView listView = (ListView) view.findViewById(R.id.addDishListView);
        adapter = new AddDishAdapter(getActivity(), list);
        listView.setAdapter(adapter);
        final Button manualOrderButton = (Button) view.findViewById(R.id.buttonAggiungi);
        final EditText price = (EditText) view.findViewById(R.id.editText_prezzo);
        final EditText name = (EditText) view.findViewById(R.id.editText_nome);
        manualOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String nameString = name.getText().toString();
                    Double priceDouble = Double.parseDouble(price.getText().toString());
                    IDish newDish;
                    if (nameString.endsWith("*")) {
                        newDish = new OrderedDish(nameString, priceDouble, OrderedDish.Moments.ZERO, 1);
                    } else {
                        newDish = new OrderedDish(nameString, priceDouble, OrderedDish.Moments.ZERO, 0);
                    }
                    Order newOrder = new Order(tableNumber, newDish, new Pair<>(1, 0));
                    new ServerDishUploader().execute(newOrder);
                } catch (NumberFormatException e) {
                    if (AddDishFragment.this.getActivity() != null) {
                        Toast.makeText(AddDishFragment.this.getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        final Button newNameButton = (Button) view.findViewById(R.id.tableNameButton);
        final EditText newNameEditText = (EditText) view.findViewById(R.id.tableNameEditText);
        newNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ServerNameUploader().execute(newNameEditText.getText().toString());
            }
        });
        new ServerMenuDownloader().execute();
        return view;
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        if (context instanceof OnAddDishFragmentInteractionListener) {
            mListener = (OnAddDishFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAddDishFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void aggiorna(final List<IDish> newList) {
        if (list != null) {
            list.clear();
            list.addAll(newList);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public interface OnAddDishFragmentInteractionListener { }

    private class AddDishAdapter extends ArrayAdapter<IDish> {

        private LayoutInflater inflater;

        public AddDishAdapter(Context context, List<IDish> persone) {
            super(context, 0, persone);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_dish_to_add, parent, false);
            }
            final IDish dish = getItem(position);
            ((TextView) convertView.findViewById(R.id.addDishName)).setText(dish.getName());
            ((TextView) convertView.findViewById(R.id.addDishPrice)).setText(String.format("%.2f", dish.getPrice()));
            final Button button = (Button) convertView.findViewById(R.id.addDishbutton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Order order = new Order(tableNumber, new OrderedDish(dish, OrderedDish.Moments.ZERO), new Pair<>(1, 0));
                    new ServerDishUploader().execute(order);
                }
            });
            return convertView;
        }
    }

    private class ServerMenuDownloader extends AsyncTask<Void, Void, List<IDish>> {

        private String ip;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (isAdded()) {
                ip = getActivity().getSharedPreferences("BenderIP", 0).getString("BenderIP", "Absent");
            } else {
                this.cancel(true);
            }
        }

        @Override
        protected List<IDish> doInBackground(Void... params) {
            //qui effettuerò la chiamata al server
            final List<IDish> temp = new LinkedList<>();
            final ServerInteractor dataDownloader = ServerInteractor.getInstance();
            final Object input = dataDownloader.sendCommandAndGetResult(ip, 6789, "GET MENU");
            if (input instanceof Exception) {
                final Exception e = (Exception) input;
                e.printStackTrace();
                Log.e("exception", e.toString());
                temp.add(new Dish(e.toString(), 0, 1));
            } else if (input instanceof IMenu) {
                final IMenu datas = (IMenu) input;
                for(final IDish d : datas.getDishesArray()) {
                    temp.add(d);
                }
            }
            return temp;
        }

        @Override
        protected void onPostExecute(List<IDish> orders) {
            super.onPostExecute(orders);
            try {
                if (isVisible()) {
                    AddDishFragment.this.aggiorna(orders);
                }
            } catch (Exception e) {
                if (isAdded()) {
                    Toast.makeText(MainActivity.toastContext, "Chiamare Jack", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private class ServerDishUploader extends AsyncTask<Order, Void, String> {

        private String output;
        private String ip;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (isAdded()) {
                output = getActivity().getString(R.string.orderAddSuccess);
                ip = getActivity().getSharedPreferences("BenderIP", 0).getString("BenderIP", "Absent");
            } else {
                this.cancel(true);
            }
        }

        @Override
        protected String doInBackground(Order... params) {
            final ServerInteractor uploader = ServerInteractor.getInstance();
            for (final Order order : params) {
                Object result = uploader.sendCommandAndGetResult(ip, 6789, order);
                if (result instanceof Exception) {
                    final Exception e = (Exception) result;
                    e.printStackTrace();
                    Log.e("exception", e.toString());
                    output = e.toString();
                    break;
                } else if (result instanceof String) {
                    final String stringResult = (String) result;
                    if (!result.equals("ORDER ADDED CORRECTLY")) {
                        output = stringResult;
                        break;
                    }
                }
            }
            return output;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (isAdded()) {
                Toast.makeText(MainActivity.toastContext, s, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ServerNameUploader extends AsyncTask<String, Void, String> {

        private String output;
        private String ip;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (isAdded()) {
                output = getActivity().getString(R.string.NameUpdateSuccess);
                ip = getActivity().getSharedPreferences("BenderIP", 0).getString("BenderIP", "Absent");
            } else {
                this.cancel(true);
            }
        }

        @Override
        protected String doInBackground(String... params) {
            final ServerInteractor uploader = ServerInteractor.getInstance();
            for (final String name : params) {
                Object result;
                if (name.length() > 0) {
                    result = uploader.sendCommandAndGetResult(ip, 6789, "SET NAME " + tableNumber + " " + name);
                } else {
                    result = uploader.sendCommandAndGetResult(ip, 6789, "REMOVE NAME " + tableNumber);
                }
                if (result instanceof Exception) {
                    final Exception e = (Exception) result;
                    e.printStackTrace();
                    Log.e("exception", e.toString());
                    output = e.toString();
                    break;
                } else if (result instanceof String) {
                    final String stringResult = (String) result;
                    if (!result.equals("NAME SET CORRECTLY")) {
                        output = stringResult;
                        break;
                    }
                }
            }
            return output;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (isAdded()) {
                Toast.makeText(MainActivity.toastContext, s, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
