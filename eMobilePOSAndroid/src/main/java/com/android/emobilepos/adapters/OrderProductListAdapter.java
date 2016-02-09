package com.android.emobilepos.adapters;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.database.ProductAddonsHandler;
import com.android.emobilepos.R;
import com.android.emobilepos.models.OrderProduct;
import com.android.emobilepos.ordering.OrderingMain_FA;
import com.android.emobilepos.ordering.PickerAddon_FA;
import com.android.support.Global;
import com.android.support.MyPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Guarionex on 1/20/2016.
 */
public class OrderProductListAdapter extends BaseAdapter {
    public enum RowType {
        TYPE_HEADER(0), TYPE_ITEM(1);
        int code;

        RowType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    private LayoutInflater mInflater;
    List<OrderProduct> orderProducts;
    int seatsAmount;
    public List<OrderSeatProduct> orderSeatProductList;
    private MyPreferences myPref;
    Activity activity;

    public OrderProductListAdapter(Activity activity, List<OrderProduct> orderProducts, int seatsAmount) {
        mInflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        myPref = new MyPreferences(activity);
        this.activity = activity;
        this.orderProducts = orderProducts;
        this.seatsAmount = seatsAmount;
        refreshList();
    }

    public void addSeat() {
        seatsAmount++;
        notifyDataSetChanged();
    }

    private List<OrderSeatProduct> getOrderSeatProductList() {
        ArrayList<OrderSeatProduct> l = new ArrayList<OrderSeatProduct>();
        if (seatsAmount > 0) {
            for (int i = 0; i < seatsAmount; i++) {
                l.add(new OrderSeatProduct(String.valueOf(i + 1)));
                for (OrderProduct product : orderProducts) {
                    if (product != null && product.assignedSeat != null &&
                            product.assignedSeat.equalsIgnoreCase(String.valueOf(i + 1))) {
                        l.add(new OrderSeatProduct(product));
                    }
                }
            }
        } else {
            for (OrderProduct product : orderProducts) {
                if (product != null) {
                    l.add(new OrderSeatProduct(product));
                }
            }
        }
        return l;
    }

    private void refreshList() {
        orderSeatProductList = getOrderSeatProductList();
    }

    @Override
    public void notifyDataSetChanged() {
        refreshList();
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return orderSeatProductList.size();
    }

    @Override
    public Object getItem(int position) {
        return orderSeatProductList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return orderSeatProductList.get(position).rowType.code;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        RowType type = orderSeatProductList.get(position).rowType;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.product_receipt_adapter, null);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        switch (type) {
            case TYPE_HEADER:
                Button menuButton = (Button) convertView.findViewById(R.id.headerMenubutton);
                menuButton.setOnClickListener((OrderingMain_FA) activity);
                convertView.findViewById(R.id.seatHeaderSection).setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.itemSection).setVisibility(View.GONE);
                ((TextView) convertView.findViewById(R.id.seatNumbertextView)).setText("Seat " + orderSeatProductList.get(position).seatNumber);
                if (OrderingMain_FA.getSelectedSeatNumber().equalsIgnoreCase(orderSeatProductList.get(position).seatNumber)) {
                    convertView.findViewById(R.id.seatHeaderSection).setBackgroundDrawable(convertView.getResources().getDrawable(R.drawable.blue_flat_button));
                } else
                    convertView.findViewById(R.id.seatHeaderSection).setBackgroundDrawable(convertView.getResources().getDrawable(R.drawable.blue_gradient_header_horizontal));
                break;
            case TYPE_ITEM:
                convertView.setBackgroundDrawable(null);
                convertView.findViewById(R.id.seatHeaderSection).setVisibility(View.GONE);
                convertView.findViewById(R.id.itemSection).setVisibility(View.VISIBLE);
                holder.itemQty = (TextView) convertView.findViewById(R.id.itemQty);
                holder.itemName = (TextView) convertView.findViewById(R.id.itemName);
                holder.itemAmount = (TextView) convertView.findViewById(R.id.itemAmount);
                holder.distQty = (TextView) convertView.findViewById(R.id.distQty);
                holder.distAmount = (TextView) convertView.findViewById(R.id.distAmount);
                holder.granTotal = (TextView) convertView.findViewById(R.id.granTotal);

                holder.addonButton = (Button) convertView.findViewById(R.id.addonButton);
                if (holder.addonButton != null)
                    holder.addonButton.setFocusable(false);
                if (orderSeatProductList.get(position).rowType == RowType.TYPE_ITEM) {
                    setHolderValues(holder, position);
                }
                break;
        }
        convertView.setTag(holder);

        return convertView;
    }

    public class OrderSeatProduct {
        public RowType rowType;
        public String seatNumber;
        public OrderProduct orderProduct;

        public OrderSeatProduct(String seatNumber) {
            this.seatNumber = seatNumber;
            this.rowType = RowType.TYPE_HEADER;
        }

        public OrderSeatProduct(OrderProduct orderProduct) {
            this.orderProduct = orderProduct;
            this.rowType = RowType.TYPE_ITEM;
        }
    }


    public void setHolderValues(ViewHolder holder, final int pos) {
        final OrderProduct product = orderSeatProductList.get(pos).orderProduct;
        final String tempId = product.ordprod_id;

        if (!myPref.getPreferences(MyPreferences.pref_restaurant_mode) || (myPref.getPreferences(MyPreferences.pref_restaurant_mode) && (Global.addonSelectionMap == null || (Global.addonSelectionMap != null && !Global.addonSelectionMap.containsKey(tempId))))) {
            if (holder.addonButton != null)
                holder.addonButton.setVisibility(View.INVISIBLE);
        } else {
            if (holder.addonButton != null) {
                holder.addonButton.setVisibility(View.VISIBLE);
                holder.addonButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(activity, PickerAddon_FA.class);
                        String prodID = product.prod_id;
//                        global.addonSelectionType = Global.addonSelectionMap.get(tempId);

                        intent.putExtra("addon_map_key", tempId);
                        intent.putExtra("isEditAddon", true);
                        intent.putExtra("prod_id", prodID);
                        intent.putExtra("item_position", pos);


                        ProductAddonsHandler prodAddonsHandler = new ProductAddonsHandler(activity);
                        Global.productParentAddons = prodAddonsHandler.getParentAddons(prodID);

                        activity.startActivityForResult(intent, 0);
                    }
                });
            }
        }

        holder.itemQty.setText(product.ordprod_qty);
        holder.itemName.setText(product.ordprod_name);

        String temp = Global.formatNumToLocale(Double.parseDouble(product.overwrite_price));
        holder.itemAmount.setText(Global.getCurrencyFormat(temp));


        holder.distQty.setText(product.disAmount);
        temp = Global.formatNumToLocale(Double.parseDouble(product.disTotal));
        holder.distAmount.setText(Global.getCurrencyFormat(temp));

        temp = Global.formatNumToLocale(Double.parseDouble(product.itemTotal));
        holder.granTotal.setText(Global.getCurrencyFormat(temp));

    }

    public class ViewHolder {
        TextView itemQty;
        TextView itemName;
        TextView itemAmount;
        TextView distQty;
        TextView distAmount;
        TextView granTotal;
        Button addonButton;
    }


}


