package com.android.gscaparrotti.bendermobile.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gscaparrotti.bendermobile.R;
import com.android.gscaparrotti.bendermobile.activities.MainActivity;
import com.android.gscaparrotti.bendermobile.network.ServerInteractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import model.Dish;
import model.IDish;
import model.Order;
import model.OrderedDish;
import model.Pair;

public class TableFragment extends Fragment {

    private static final String TABLE_NUMBER = "TABLENMBR";
    private int tableNumber;
    private List<Order> list = new LinkedList<>();
    private DishAdapter adapter;
    private Timer timer;

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
        if (tableNumber > 0) {
            text.setText(text.getText() + " " + Integer.toString(tableNumber));
        } else if (tableNumber == 0) {
            text.setText(getString(R.string.ViewAllPendingOrders));
            Button add = (Button) view.findViewById(R.id.addToTable);
            add.setEnabled(false);
        }
        ListView listView = (ListView) view.findViewById(R.id.dishesList);
        adapter = new DishAdapter(getActivity(), list);
        listView.setAdapter(adapter);
        Button update = (Button) view.findViewById(R.id.updateButton);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateAndStartTasks();
            }
        });
        Button addDish = (Button) view.findViewById(R.id.addToTable);
        addDish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onAddDishEventFired(tableNumber);
                }
            }
        });
        CheckBox filter = (CheckBox) view.findViewById(R.id.filterCheckBox);
        if (tableNumber == 0) {
            filter.setVisibility(View.VISIBLE);
        }
        filter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (TableFragment.this.isVisible() && list != null) {
                    aggiorna(new ArrayList<>(list));
                    if (!isChecked) {
                        new ServerOrdersDownloader().execute(tableNumber);
                    }
                }
            }
        });
        if (tableNumber == 0) {
            addDish.setClickable(false);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("FRAGMENT ON RESUME", "FRAGMENT ON RESUME");
        updateAndStartTasks();
    }

    @Override
    public void onAttach(Activity context) {
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
            final CheckBox filter = (CheckBox) getView().findViewById(R.id.filterCheckBox);
            if (filter.isChecked()) {
                for (final Order o : newList) {
                    if (o.getDish().getFilterValue() != 0) {
                        list.add(o);
                    }
                }
            } else {
                list.addAll(newList);
            }
            if (tableNumber != 0) {
                Collections.sort(list, new Comparator<Order>() {
                    @Override
                    public int compare(Order o1, Order o2) {
                        return (o2.getAmounts().getX() - o2.getAmounts().getY()) - (o1.getAmounts().getX() - o1.getAmounts().getY());
                    }
                });
            } else {
                Collections.sort(list, new Comparator<Order>() {
                    @Override
                    public int compare(Order o1, Order o2) {
                        if (o1.getDish() instanceof OrderedDish && o2.getDish() instanceof OrderedDish) {
                            return (((OrderedDish) o1.getDish()).getTime().compareTo(((OrderedDish) o2.getDish()).getTime()));
                        } else if (o1.getDish() instanceof OrderedDish && !(o2.getDish() instanceof OrderedDish)) {
                            return -1;
                        } else if (o2.getDish() instanceof OrderedDish && !(o1.getDish() instanceof OrderedDish)){
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        double totalPrice = 0;
        for (Order o : newList) {
            totalPrice += o.getAmounts().getX() * o.getDish().getPrice();
        }
        if (getView() != null) {
            TextView price = (TextView) getView().findViewById(R.id.totalPrice);
            price.setText(getResources().getString(R.string.PrezzoTotale) + String.format("%.2f", totalPrice) + getResources().getString(R.string.valute));
        }
    }

    public void aggiornaNome (final String name) {
        if (getView() != null) {
            TextView nameView = (TextView) getView().findViewById(R.id.tableTitle);
            String newName = name.length() > 0 ? (" - " + name) : "";
            nameView.setText(getString(R.string.tableTitle) + " " + Integer.toString(tableNumber) + newName);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        super.onDestroyView();
        Log.d("FRAGMENT STOP", "FRAGMENT STOP");
        stopTasks();
    }

    private synchronized void updateAndStartTasks() {
        //if timer is running, then just update, otherwise create timer and start it
        if (timer != null) {
            new ServerOrdersDownloader().execute(tableNumber);
        } else {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                        MainActivity.runOnUI(new Runnable() {
                            @Override
                            public void run() {
                                new ServerOrdersDownloader().execute(tableNumber);
                            }
                        });
                }
            }, 0, 6000);
        }
    }

    private synchronized void stopTasks() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        timer = null;
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
        void onAddDishEventFired(final int tableNumber);
    }

    private class DishAdapter extends ArrayAdapter<Order> {

        private LayoutInflater inflater;

        public DishAdapter(Context context, List<Order> persone) {
            super(context, 0, persone);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_dish, parent, false);
            }
            final Order order = getItem(position);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final DishDetailFragment detail = DishDetailFragment.newInstance(order);
                    detail.show(getFragmentManager(), "Dialog");
                }
            });
            convertView.setLongClickable(true);
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    order.getAmounts().setY(order.getAmounts().getX());
                    if (tableNumber == 0) {
                        final IDish dish = new Dish(order.getDish().getName().substring(0, order.getDish().getName().lastIndexOf(" - ")), order.getDish().getPrice(), 0);
                        final Order newOrder = new Order(order.getTable(), dish, order.getAmounts());
                        new ServerOrdersUploader().execute(newOrder);
                    } else {
                        new ServerOrdersUploader().execute(order);
                    }
                    return true;
                }
            });
            convertView.findViewById(R.id.removeButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (tableNumber == 0) {
                        final IDish dish = new Dish(order.getDish().getName().substring(0, order.getDish().getName().lastIndexOf(" - ")), order.getDish().getPrice(), 0);
                        final Order newOrder = new Order(order.getTable(), dish, new Pair<>(-1, 1));
                        new ServerOrdersUploader().execute(newOrder);
                    } else {
                        new ServerOrdersUploader().execute(new Order(order.getTable(), order.getDish(), new Pair<>(-1, 1)));
                    }
                }
            });
            ((TextView) convertView.findViewById(R.id.dish)).setText(order.getDish().getName());
            ((TextView) convertView.findViewById(R.id.dishToServe))
                    .setText(getResources().getString(R.string.StringOrdinati) + Integer.toString(order.getAmounts().getX()));
            ((TextView) convertView.findViewById(R.id.dishServed))
                    .setText(getResources().getString(R.string.StringDaServire) + Integer.toString(order.getAmounts().getX() - order.getAmounts().getY()));
            if (!order.getAmounts().getX().equals(order.getAmounts().getY())) {
                convertView.findViewById(R.id.itemTableLayout).setBackgroundColor(Color.parseColor("#80FF5050"));
            } else {
                convertView.findViewById(R.id.itemTableLayout).setBackgroundColor(Color.parseColor("#8099FF66"));
            }
            return convertView;
        }
    }

    private class ServerOrdersUploader extends AsyncTask<Order, Void, Boolean> {

        private String errorMessage;
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
        protected Boolean doInBackground(Order... params) {
            final ServerInteractor uploader = ServerInteractor.getInstance();
            boolean result = false;
            final Object resultFromServer = uploader.sendCommandAndGetResult(ip, 6789, params[0]);
            if (resultFromServer instanceof Exception) {
                final Exception e = (Exception) resultFromServer;
                errorMessage = e.toString();
            } else if (resultFromServer instanceof String) {
                final String stringResult = (String) resultFromServer;
                if (stringResult.equals("ORDER UPDATED CORRECTLY")) {
                    result = true;
                } else {
                    errorMessage = stringResult;
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean) {
                if (isVisible()) {
                    Toast.makeText(MainActivity.toastContext, MainActivity.toastContext.getString(R.string.UpdateSuccess), Toast.LENGTH_SHORT).show();
                }
                new ServerOrdersDownloader().execute(tableNumber);
            } else {
                final List<Order> errors = new LinkedList<>();
                errors.add(new Order(TableFragment.this.tableNumber, new Dish(errorMessage, 0, 1), new Pair<>(0, 1)));
                try {
                    if (isVisible()) {
                        aggiorna(errors);
                    }
                } catch (Exception e) {
                    if (!(e instanceof NullPointerException) && isAdded()) {
                        Toast.makeText(MainActivity.toastContext, "Chiamare Jack", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    private class ServerOrdersDownloader extends AsyncTask<Integer, Void, Pair<List<Order>, String>> {

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
        protected Pair<List<Order>, String> doInBackground(Integer... params) {
            //qui effettuerò la chiamata al server
            final List<Order> temp = new LinkedList<>();
            Map<Integer, String> names = new HashMap<>();
            String tableName = null;
            final ServerInteractor dataDownloader = ServerInteractor.getInstance();
            Object input = null;
            if (tableNumber > 0) {
                input = dataDownloader.sendCommandAndGetResult(ip, 6789, "GET TABLE " + params[0]);
            } else if (tableNumber == 0) {
                input = dataDownloader.sendCommandAndGetResult(ip, 6789, "GET PENDING ORDERS");
            } else {
                //noinspection ThrowableInstanceNeverThrown
                input = new Exception("Invalid Table Number");
            }
            final Object tableNameInput = dataDownloader.sendCommandAndGetResult(ip, 6789, "GET NAMES");
            if (tableNameInput instanceof Map) {
                //noinspection unchecked
                names = (Map<Integer, String>) tableNameInput;
            } else if (tableNameInput instanceof Exception) {
                final Exception e = (Exception) tableNameInput;
                Log.e("exception", e.toString());
                temp.add(new Order(TableFragment.this.tableNumber, new Dish(e.toString(), 0, 1), new Pair<>(0, 1)));
                stopTasks();
                return new Pair<>(temp, tableName);
            }
            if (input instanceof Exception) {
                final Exception e = (Exception) input;
                e.printStackTrace();
                Log.e("exception", e.toString());
                temp.add(new Order(TableFragment.this.tableNumber, new Dish(e.toString(), 0, 1), new Pair<>(0, 1)));
                stopTasks();
                return new Pair<>(temp, tableName);
            } else if (input instanceof Map) {
                //noinspection unchecked
                final Map<IDish, Pair<Integer, Integer>> datas = (Map<IDish, Pair<Integer, Integer>>) input;
                for(final Map.Entry<IDish, Pair<Integer, Integer>> entry : datas.entrySet()) {
                    temp.add(new Order(TableFragment.this.tableNumber, entry.getKey(), entry.getValue()));
                }
                tableName = names.get(TableFragment.this.tableNumber);
            } else if (input instanceof List) {
                //noinspection unchecked
                final List<Order> datas = (List<Order>) input;
                for (final Order o : datas) {
                    final StringBuilder strbldr = new StringBuilder(o.getDish().getName());
                    strbldr.append(" - ").append(o.getTable());
                    if (names.containsKey(o.getTable())) {
                        strbldr.append(" (").append(names.get(o.getTable())).append(")");
                    }
                    if (o.getDish() instanceof OrderedDish) {
                        final OrderedDish originalDish = (OrderedDish) o.getDish();
                        final OrderedDish tempDish = new OrderedDish(strbldr.toString(), o.getDish().getPrice(), originalDish.getFilterValue(), originalDish);
                        temp.add(new Order(o.getTable(), tempDish, o.getAmounts()));
                    } else {
                        temp.add(new Order(o.getTable(), new Dish(strbldr.toString(), o.getDish().getPrice(), o.getDish().getFilterValue()), o.getAmounts()));
                    }
                }
            }
            return new Pair<>(temp, tableName);
        }

        @Override
        protected void onPostExecute(Pair<List<Order>, String> orders) {
            super.onPostExecute(orders);
            try {
                if (isVisible()) {
                    aggiorna(orders.getX());
                    aggiornaNome(orders.getY() != null ? orders.getY() : "");
                }
            } catch (Exception e) {
                if (!(e instanceof NullPointerException) && isAdded()) {
                    Toast.makeText(MainActivity.toastContext, "Chiamare Jack", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
