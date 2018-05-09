package com.android.gscaparrotti.bendermobile.utilities;

import android.app.Fragment;

import com.android.gscaparrotti.bendermobile.network.BenderNetworkException;

public abstract class FragmentNetworkingBenderAsyncTask<INPUT, OUTPUT> extends BenderAsyncTask<INPUT, OUTPUT> {

    protected String ip;
    private final Fragment fragment;

    public FragmentNetworkingBenderAsyncTask(final Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    protected final void onPreExecute() {
        super.onPreExecute();
        if (fragment.isAdded()) {
            this.ip = fragment.getActivity().getSharedPreferences("BenderIP", 0).getString("BenderIP", "Absent");
            innerOnPreExecute();
        } else {
            this.cancel(true);
        }
    }

    @Override
    protected final BenderAsyncTaskResult<OUTPUT> doInBackground(final INPUT[] objects) {
        try {
            return innerDoInBackground(objects);
        } catch (final BenderNetworkException e) {
            return new BenderAsyncTaskResult<OUTPUT>(e);
        }
    }

    @Override
    protected final void onPostExecute(final BenderAsyncTaskResult<OUTPUT> result) {
        super.onPostExecute(result);
        if (fragment.isAdded()) {
            if (result.isSuccess()) {
                innerOnSuccessfulPostExecute(result);
            } else {
                innerOnUnsuccessfulPostExecute(result);
            }
        }
    }

    protected void innerOnPreExecute() {
    }

    protected abstract BenderAsyncTaskResult<OUTPUT> innerDoInBackground(final INPUT[] objects);

    protected abstract void innerOnSuccessfulPostExecute(final BenderAsyncTaskResult<OUTPUT> result);

    protected abstract void innerOnUnsuccessfulPostExecute(final BenderAsyncTaskResult<OUTPUT> error);

}
