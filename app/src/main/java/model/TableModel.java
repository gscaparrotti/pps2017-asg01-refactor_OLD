package model;

import com.android.gscaparrotti.bendermobile.MainActivity;
import com.android.gscaparrotti.bendermobile.MainFragment;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by gscap_000 on 20/04/2016.
 */
public class TableModel {

    private MainFragment ta;
    private List<Integer> list = new LinkedList<>();

    public void addAll(final MainFragment ta) {
        if (ta == null) {
            this.ta = ta;
        }
        for (int i = 0; i<50; i++) {
            list.add(i);
            ta.tableAdded(list.get(i));
        }
    }

    public List<Integer> getAll() {
        return new LinkedList<>(list);
    }
}
