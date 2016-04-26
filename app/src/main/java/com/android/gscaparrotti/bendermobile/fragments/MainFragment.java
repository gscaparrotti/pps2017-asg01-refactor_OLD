package com.android.gscaparrotti.bendermobile.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gscaparrotti.bendermobile.R;
import com.android.gscaparrotti.bendermobile.activities.MainActivity;
import com.android.gscaparrotti.bendermobile.network.ServerInteractor;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnMainFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {

    private GridView gv;
    private TableAdapter ta;

    private OnMainFragmentInteractionListener mListener;

    public MainFragment() {
    }

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        gv = (GridView) view.findViewById(R.id.tablesContainer);
        ta = new TableAdapter(getActivity());
        gv.setAdapter(ta);
        new TableAmountDownloader().execute();
        view.findViewById(R.id.mainUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TableAmountDownloader().execute();
            }
        });
        view.findViewById(R.id.allPending).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onTablePressedEventFired(0);
            }
        });
        return view;
    }

    public void tableAdded(final int tableNumber, final Map<Integer, String> names) {
        ta.reset();
        for (int i = 0; i < tableNumber; i++) {
            ta.addElement(i + 1, names.get(i + 1));
        }
        ta.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        if (context instanceof OnMainFragmentInteractionListener) {
            mListener = (OnMainFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMainFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnMainFragmentInteractionListener {
        void onTablePressedEventFired(int tableNumber);
    }

    private class TableAdapter extends BaseAdapter {

        private int n = 0;
        private Map<Integer, String> names = new HashMap<>();
        private LayoutInflater inflater;

        TableAdapter(Context context) {
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return n;
        }

        public void addElement(final Integer i, final String name) {
            n++;
            names.put(i, name);
        }

        public void reset() {
            n = 0;
            names.clear();
        }

        @Override
        public Integer getItem(int position) {
            return position + 1;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_table, parent, false);
            }
            final Integer table = getItem(position);
            final TextView tableView = (TextView) convertView.findViewById(R.id.table);
            tableView.setText(getString(R.string.itemTableText) + table + formattedName(names.get(table)));
            convertView.setLongClickable(true);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onTablePressedEventFired(table);
                }
            });
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.ResetConfirmDialogTitle))
                            .setMessage(R.string.ResetConfirmDialogQuestion)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    new TableResetRequestUploader().execute(table);
                                    new TableAmountDownloader().execute();
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                    return true;
                }
            });
            return convertView;
        }

        private String formattedName(final String name) {
            return !(name == null) ? " - " + name : "";
        }
    }

    private class TableResetRequestUploader extends AsyncTask<Integer, Void, Boolean> {

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
        protected Boolean doInBackground(final Integer... params) {
            final ServerInteractor serverInteractor = ServerInteractor.getInstance();
            final String command = "RESET TABLE " + params[0];
            boolean success = false;
            final Object input = serverInteractor.sendCommandAndGetResult(ip, 6789, command);
            if (input instanceof Exception) {
                final Exception e = (Exception) input;
                errorMessage = e.toString();
            } else if (input instanceof String) {
                final String stringInput = (String) input;
                if (stringInput.equals("TABLE RESET CORRECTLY")) {
                    success = true;
                } else {
                    errorMessage = stringInput;
                }
            }
            return success;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            super.onPostExecute(success);
            if (isAdded()) {
                if (success) {
                    Toast.makeText(MainActivity.toastContext, getString(R.string.ResetSuccess), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.toastContext, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private class TableAmountDownloader extends AsyncTask<Void, Void, Pair<Integer, Map<Integer, String>>> {

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
        protected Pair<Integer, Map<Integer, String>> doInBackground(final Void... params) {
            final ServerInteractor serverInteractor = ServerInteractor.getInstance();
            Integer amount = 0;
            Map<Integer, String> names = ImmutableMap.of();
            final Object receivedAmount = serverInteractor.sendCommandAndGetResult(ip, 6789, "GET AMOUNT");
            if (receivedAmount instanceof Exception) {
                amount = -1;
            } else if (receivedAmount instanceof Integer){
                amount = (Integer) receivedAmount;
                final Object receivedNames = serverInteractor.sendCommandAndGetResult(ip, 6789, "GET NAMES");
                if (receivedNames instanceof Exception) {
                    amount = -1;
                } else if (receivedNames instanceof Map) {
                    names = (Map<Integer, String>) receivedNames;
                }
            }
            return new Pair<>(amount, names);
        }

        @Override
        protected void onPostExecute(final Pair<Integer, Map<Integer, String>> pair) {
            super.onPostExecute(pair);
            if (isAdded()) {
                if (pair.first < 0) {
                    Toast.makeText(MainActivity.toastContext, getString(R.string.ServerError), Toast.LENGTH_LONG).show();
                    if (isVisible()) {
                        MainFragment.this.getView().setBackgroundColor(Color.rgb(204, 94, 61));
                    }
                } else {
                    if (isVisible()) {
                        MainFragment.this.getView().setBackgroundColor(Color.TRANSPARENT);
                        MainFragment.this.tableAdded(pair.first, pair.second);
                    }
                }
            }
        }
    }
}
