package net.osmand.plus.activities.search;


import java.util.List;

import net.osmand.OsmAndFormatter;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchHistoryHelper.HistoryEntry;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class SearchHistoryActivity extends ListActivity {
	private LatLon location;
	private SearchHistoryHelper helper;
	private Button clearButton;
	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT;
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView lv = new ListView(this);
		lv.setId(android.R.id.list);
		
		setContentView(lv);
		Intent intent = getIntent();
		if(intent != null){
			float lat = intent.getFloatExtra(SEARCH_LAT, 0);
			float lon = intent.getFloatExtra(SEARCH_LON, 0);
			if(lat != 0 || lon != 0){
				location = new LatLon(lat, lon);
			}
		}
		if (location == null) {
			location = OsmandSettings.getOsmandSettings(this).getLastKnownMapLocation();
		}
		helper = SearchHistoryHelper.getInstance();
		
		
		clearButton = new Button(this);
		clearButton.setText(R.string.clear_all);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				helper.removeAll(SearchHistoryActivity.this);
				setListAdapter(new HistoryAdapter(helper.getHistoryEntries(SearchHistoryActivity.this)));
			}
		});
		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
				return SearchHistoryActivity.this.onItemLongClick(pos);
			}

		});
	}
	@Override
	protected void onResume() {
		super.onResume();
		List<HistoryEntry> historyEntries = helper.getHistoryEntries(this);
		
		getListView().removeFooterView(clearButton);
		if (!historyEntries.isEmpty()) {
			getListView().addFooterView(clearButton);
		}
		setListAdapter(new HistoryAdapter(historyEntries));
	}
	
	private boolean onItemLongClick(int pos) {
		final HistoryEntry entry = ((HistoryAdapter) getListAdapter()).getItem(pos);
		AlertDialog.Builder builder = new AlertDialog.Builder(SearchHistoryActivity.this);
		builder.setTitle(entry.getName());
		builder.setItems(new String[] { getString(R.string.show_poi_on_map), getString(R.string.navigate_to) },
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {
							OsmandSettings.getOsmandSettings(SearchHistoryActivity.this).setMapLocationToShow(entry.getLat(), entry.getLon());
						} else if (which == 1) {
							OsmandSettings.getOsmandSettings(SearchHistoryActivity.this).setPointToNavigate(entry.getLat(), entry.getLon(), null);
						}

						MapActivity.launchMapActivityMoveToTop(SearchHistoryActivity.this);

					}

				});
		builder.show();
		return true;
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		HistoryEntry model = ((HistoryAdapter)getListAdapter()).getItem(position);
		selectModel(model);
	}

	private void selectModel(HistoryEntry model) {
		helper.selectEntry(model, this);
		OsmandSettings.getOsmandSettings(SearchHistoryActivity.this).setMapLocationToShow(model.getLat(), model.getLon());
		MapActivity.launchMapActivityMoveToTop(this);
	}
	
	
	class HistoryAdapter extends ArrayAdapter<HistoryEntry> {

		public HistoryAdapter(List<HistoryEntry> list) {
			super(SearchHistoryActivity.this, R.layout.search_history_list_item, list);
		}
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.search_history_list_item, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.label);
			TextView distanceLabel = (TextView) row.findViewById(R.id.distance_label);
			ImageButton icon = (ImageButton) row.findViewById(R.id.remove);
			final HistoryEntry model = getItem(position);
			if(location != null){
				int dist = (int) (MapUtils.getDistance(location, model.lat, model.lon));
				distanceLabel.setText(OsmAndFormatter.getFormattedDistance(dist, SearchHistoryActivity.this));
			} else {
				distanceLabel.setText(""); //$NON-NLS-1$
			}
			label.setText(model.name);
			icon.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					helper.remove(model, SearchHistoryActivity.this);
					setListAdapter(new HistoryAdapter(helper.getHistoryEntries(SearchHistoryActivity.this)));
				}
				
			});
			View.OnClickListener clickListener = new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					selectModel(model);
				}
			};
			
			View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					return onItemLongClick(position);
				}
			};
			distanceLabel.setOnLongClickListener(longClickListener);
			label.setOnLongClickListener(longClickListener);
			distanceLabel.setOnClickListener(clickListener);
			label.setOnClickListener(clickListener);
			return row;
		}
	}

}
