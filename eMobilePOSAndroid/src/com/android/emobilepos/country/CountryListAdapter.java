package com.android.emobilepos.country;

import java.util.List;

import com.emobilepos.app.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


public class CountryListAdapter extends BaseAdapter {

	private Context context;
	List<Country> countries;
	LayoutInflater inflater;

	public CountryListAdapter(Context context, List<Country> countries) {
		super();
		this.context = context;
		this.countries = countries;
		inflater = (LayoutInflater) this.context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return countries.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Return row for each country
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View cellView = convertView;
		Cell cell;
		Country country = countries.get(position);

		if (convertView == null) {
			cell = new Cell();
			cellView = inflater.inflate(R.layout.country_picker_adapter, null);
			cell.textView = (TextView) cellView.findViewById(R.id.row_title);
			cellView.setTag(cell);
		} else {
			cell = (Cell) cellView.getTag();
		}

		cell.textView.setText(country.getName());
		return cellView;
	}

	/**
	 * Holder for the cell
	 * 
	 */
	static class Cell {
		public TextView textView;
	}

}