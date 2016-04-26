package com.android.gscaparrotti.bendermobile;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements TableFragment.OnTableFragmentInteractionListener, MainFragment.OnMainFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        replaceFragment(MainFragment.newInstance(), false);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0)
            getFragmentManager().popBackStack();
        else
            super.onBackPressed();

    }

    private void replaceFragment(Fragment fragment, boolean back) {
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.container, fragment);
        if (back)
            transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onTablePressed(int tableNumber) {
        replaceFragment(TableFragment.newInstance(tableNumber), true);
    }


    @Override
    public void onLoadingInProgress() {

    }
}
