package com.android.emobilepos.ordering;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.database.ProductsHandler;
import com.android.database.ProductsImagesHandler;
import com.android.emobilepos.R;
import com.android.emobilepos.ShowProductImageActivity;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.android.support.fragmentactivity.BaseFragmentActivityActionBar;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ViewProductDetails_FA extends BaseFragmentActivityActionBar implements OnClickListener {

    private List<String> infoTitle;
    private List<String> inventTitle;
    private List<String> identTitle;

    private List<String> infoInfo = new ArrayList<>();
    private List<String> inventInfo = new ArrayList<>();
    private List<String> identInfo = new ArrayList<>();
    private List<String> attributes = new ArrayList<>();
    private Global global;
    private boolean hasBeenCreated = false;

    private String vidLink = "", img_url = "";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.catalog_proddetails_layout);
        ListView myListView = (ListView) findViewById(R.id.catalogProdDetailsLV);

        infoTitle = Arrays.asList(getAString(R.string.catalog_name), getAString(R.string.catalog_description),
                getAString(R.string.cat_details_extra_desc), getAString(R.string.cat_details_prod_det_type), getAString(R.string.cat_details_tax_code),
                getAString(R.string.cat_details_price), getAString(R.string.cat_details_dis_type)
                , (getAString(R.string.redeem) + " Points"), (getAString(R.string.accumulable) + " Points"));
        inventTitle = Collections.singletonList(getAString(R.string.cat_picker_onhand));
        identTitle = Arrays.asList(getAString(R.string.cat_details_sku), getAString(R.string.cat_details_cat_id),
                getAString(R.string.cat_details_upc));

        ListViewAdapter adapter = new ListViewAdapter(this);
        global = (Global) getApplication();


        Bundle extras = this.getIntent().getExtras();
        img_url = extras.getString("url");
        final String prod_id = extras.getString("prod_id");

        View header = getLayoutInflater().inflate(R.layout.catalog_proddetails_header, (ViewGroup) findViewById(R.id.header_layout_root));
        TextView videoLink = (TextView) header.findViewById(R.id.catalogVideoLink);
        ImageView productImg = (ImageView) header.findViewById(R.id.catalogHeaderImage);

        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(this));
        DisplayImageOptions options = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.loading_image).cacheInMemory(false).cacheOnDisc(true)
                .showImageForEmptyUri(R.drawable.no_image).build();
        imageLoader.displayImage(img_url, productImg, options);

        ProductsHandler prodHandler = new ProductsHandler(this);

        infoInfo = prodHandler.getProdInformation(prod_id);
        inventInfo = prodHandler.getProdInventory(prod_id);
        identInfo = prodHandler.getProdIdentification(prod_id);
        attributes = Collections.singletonList("");
        ProductsImagesHandler imgHandler = new ProductsImagesHandler(this);
        vidLink = imgHandler.getSpecificLink("V", prod_id);

        if (!vidLink.isEmpty()) {
            videoLink.setText(R.string.view_video);
            videoLink.setOnClickListener(this);
        } else {
            videoLink.setText(R.string.no_video);
        }

        productImg.setOnClickListener(this);


        myListView.addHeaderView(header);

        myListView.setAdapter(adapter);
        hasBeenCreated = true;

    }


    @Override
    public void onResume() {

        if (global.isApplicationSentToBackground())
            Global.loggedIn = false;
        global.stopActivityTransitionTimer();

        if (hasBeenCreated && !Global.loggedIn) {
            if (global.getGlobalDlog() != null)
                global.getGlobalDlog().dismiss();
            global.promptForMandatoryLogin(this);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
//        MyPreferences myPref = new MyPreferences(this);
//        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
//        boolean isScreenOn = powerManager.isScreenOn();
//        if (!isScreenOn && myPref.isExpireUserSession())
//            Global.loggedIn = false;
        global.startActivityTransitionTimer();
    }


    private String getAString(int id) {
        Resources resources = getResources();
        return resources.getString(id);
    }

    public void showPrompt(String title, String msg) {
        final Dialog dlog = new Dialog(this, R.style.Theme_TransparentTest);
        dlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlog.setCancelable(true);
        dlog.setCanceledOnTouchOutside(true);
        dlog.setContentView(R.layout.dlog_btn_single_layout);

        TextView viewTitle = (TextView) dlog.findViewById(R.id.dlogTitle);
        TextView viewMsg = (TextView) dlog.findViewById(R.id.dlogMessage);
        viewTitle.setText(title);
        viewMsg.setText(msg);
        Button btnOk = (Button) dlog.findViewById(R.id.btnDlogSingle);
        btnOk.setText(R.string.button_ok);
        btnOk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dlog.dismiss();
            }
        });
        dlog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.catalogVideoLink:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(vidLink)));
                break;
            case R.id.catalogHeaderImage:
                Intent intent = new Intent(this, ShowProductImageActivity.class);
                intent.putExtra("url", img_url);
                startActivity(intent);
                break;
        }
    }

    private class ListViewAdapter extends BaseAdapter {

        LayoutInflater myInflater;
        private int size1;
        private int size2;
        private int size3;


        ListViewAdapter(Context context) {
            myInflater = LayoutInflater.from(context);
            size1 = infoTitle.size();
            size2 = inventTitle.size();
            size3 = identTitle.size();
            //size4 = attributes.size();
        }

        @Override
        public int getCount() {
            return infoTitle.size() + inventTitle.size() + identTitle.size() + 4;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            int type = getItemViewType(position);
            final int index = position;
            if (convertView == null) {
                holder = new ViewHolder();
                switch (type) {
                    case 0:// dividers
                    {
                        convertView = myInflater.inflate(R.layout.orddetails_lvdivider_adapter, null);
                        holder.leftTitle = (TextView) convertView.findViewById(R.id.orderDivLeft);
                        if (position == 0) {
                            holder.leftTitle.setText(R.string.information);
                        } else if (position == (size1 + 1)) {
                            holder.leftTitle.setText(R.string.inventory);
                        } else if (position == (size1 + size2 + 2)) {
                            holder.leftTitle.setText(R.string.identification);
                        } else {
                            holder.leftTitle.setText(R.string.attributes);
                        }

                        break;
                    }
                    case 1: {
                        convertView = myInflater.inflate(R.layout.catalog_proddetails_adapter1, null);
                        holder.leftTitle = (TextView) convertView.findViewById(R.id.prodDetailsTitle);
                        holder.leftSubtitle = (TextView) convertView.findViewById(R.id.prodDetailsSubtitle);

                        holder.leftTitle.setText(infoTitle.get(position - 1));
                        holder.leftSubtitle.setText(infoInfo.get(position - 1));
                        break;
                    }
                    case 2: {
                        convertView = myInflater.inflate(R.layout.catalog_proddetails_adapter2, null);
                        holder.leftTitle = (TextView) convertView.findViewById(R.id.prodDetailsTitle);
                        holder.leftSubtitle = (TextView) convertView.findViewById(R.id.prodDetailsSubtitle);
                        holder.moreInfoIcon = (ImageView) convertView.findViewById(R.id.prodMoreDetailsIcon);

                        holder.leftTitle.setText(infoTitle.get(position - 1));
                        holder.leftSubtitle.setText(infoInfo.get(position - 1));

                        holder.moreInfoIcon.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                showPrompt(infoTitle.get(index - 1), infoInfo.get(index - 1));
                            }
                        });
                        break;
                    }
                    case 3: {
                        convertView = myInflater.inflate(R.layout.catalog_proddetails_adapter3, null);
                        holder.leftTitle = (TextView) convertView.findViewById(R.id.prodDetailsLeft);
                        holder.rightTitle = (TextView) convertView.findViewById(R.id.prodDetailsRight);
                        if (position > 3 && position <= (size1)) {
                            holder.leftTitle.setText(infoTitle.get(position - 1));
                            holder.rightTitle.setText(infoInfo.get(position - 1));
                        } else if (position <= size1 + size2 + 2) {
                            holder.leftTitle.setText(inventTitle.get(position - (size1 + 2)));
                            holder.rightTitle.setText(inventInfo.get(position - (size1 + 2)));
                        } else if (position <= size1 + size2 + size3 + 3) {
                            holder.leftTitle.setText(identTitle.get(position - (size1 + size2 + 3)));
                            holder.rightTitle.setText(identInfo.get(position - (size1 + size2 + 3)));
                        } else {
                            holder.leftTitle.setText(attributes.get(position - (size1 + size2 + size3 + 4)));
                        }
                        break;
                    }
                }
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (type == 0) {
                if (position == 0) {
                    holder.leftTitle.setText(getAString(R.string.cat_details_info));
                } else if (position == (size1 + 1)) {
                    holder.leftTitle.setText(getAString(R.string.cat_details_inventory));
                } else if (position == (size1 + size2 + 2)) {
                    holder.leftTitle.setText(getAString(R.string.cat_details_identification));
                } else {
                    holder.leftTitle.setText(getAString(R.string.cat_picker_attributes));
                }
            } else if (type == 1) {
                holder.leftTitle.setText(infoTitle.get(position - 1));
                holder.leftSubtitle.setText(infoInfo.get(position - 1));
            } else if (type == 2) {
                holder.leftTitle.setText(infoTitle.get(position - 1));
                holder.leftSubtitle.setText(infoInfo.get(position - 1));

                holder.moreInfoIcon.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        showPrompt(infoTitle.get(index - 1), infoInfo.get(index - 1));
                    }
                });

            } else {
                if (position > 3 && position <= (size1)) {
                    holder.leftTitle.setText(infoTitle.get(position - 1));
                    holder.rightTitle.setText(infoInfo.get(position - 1));
                } else if (position <= size1 + size2 + 2) {
                    holder.leftTitle.setText(inventTitle.get(position - (size1 + 2)));
                    holder.rightTitle.setText(inventInfo.get(position - (size1 + 2)));
                } else if (position <= size1 + size2 + size3 + 3) {
                    holder.leftTitle.setText(identTitle.get(position - (size1 + size2 + 3)));
                    holder.rightTitle.setText(identInfo.get(position - (size1 + size2 + 3)));
                } else {
                    holder.leftTitle.setText(attributes.get(position - (size1 + size2 + size3 + 4)));
                }
            }
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 || (position == size1 + 1) || (position == (size1 + size2 + 2)) || (position == (size1 + size2 + size3 + 3))) {

                return 0;
            } else if (position == 1) {
                return 1;
            } else if (position == 2 || position == 3) {
                return 2;
            }

            return 3;

        }

        @Override
        public int getViewTypeCount() {
            return 5;
        }

        class ViewHolder {
            TextView leftTitle;
            TextView leftSubtitle;
            TextView rightTitle;
            ImageView moreInfoIcon;
        }
    }

}
