package com.android.emobilepos.ordering;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.database.DBManager;
import com.android.database.MemoTextHandler;
import com.android.database.OrderProductsHandler;
import com.android.database.OrderTaxes_DB;
import com.android.database.OrdersHandler;
import com.android.database.ProductsHandler;
import com.android.emobilepos.R;
import com.android.emobilepos.adapters.OrderProductListAdapter;
import com.android.emobilepos.adapters.SplittedOrderSummaryAdapter;
import com.android.emobilepos.models.OrderProduct;
import com.android.emobilepos.models.OrderSeatProduct;
import com.android.emobilepos.models.SplitedOrder;
import com.android.emobilepos.payment.SelectPayMethod_FA;
import com.android.support.DateUtils;
import com.android.support.GenerateNewID;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.android.support.TaxesCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Guarionex on 2/19/2016.
 */
public class SplittedOrderDetailsFR extends Fragment implements View.OnClickListener {


    private TextView orderId;
    private TextView subtotal;
    private MyPreferences myPref;
    private TextView globalDiscountTextView;
    private TextView lineItemDiscountTotal;

    private TextView taxTotal;
    private TextView granTotal;
    private LinearLayout productAddonsSection;
    private LinearLayout orderProductSection;
    private LayoutInflater inflater;

    public SplitedOrder restaurantSplitedOrder;
    private LinearLayout receiptPreview;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View detailView = inflater.inflate(R.layout.splitted_order_detail_fragment,
                container, false);
        this.inflater = inflater;
        myPref = new MyPreferences(getActivity());
        MemoTextHandler handler = new MemoTextHandler(getActivity());
        String[] header = handler.getHeader();
        String[] footer = handler.getFooter();
        Button checkoutBtn = (Button) detailView.findViewById(R.id.checkoutbutton);
        Button printReceiptBtn = (Button) detailView.findViewById(R.id.printReceiptbutton2);
        Button printAllReceiptBtn = (Button) getActivity().findViewById(R.id.printAllReceiptbutton3);
        printAllReceiptBtn.setOnClickListener(this);
        checkoutBtn.setOnClickListener(this);
        printReceiptBtn.setOnClickListener(this);
        TextView header1 = (TextView) detailView.findViewById(R.id.memo_headerLine1textView);
        TextView header2 = (TextView) detailView.findViewById(R.id.memo_headerLine2textView16);
        TextView header3 = (TextView) detailView.findViewById(R.id.memo_headerLine3textView18);
        orderId = (TextView) detailView.findViewById(R.id.orderIdtextView16);
        TextView orderDate = (TextView) detailView.findViewById(R.id.orderDatetextView21);
        TextView deviceName = (TextView) detailView.findViewById(R.id.deviceNametextView23);
        subtotal = (TextView) detailView.findViewById(R.id.subtotaltextView);
        lineItemDiscountTotal = (TextView) detailView.findViewById(R.id.lineitem_discounttextView);
        globalDiscountTextView = (TextView) detailView.findViewById(R.id.globaldiscounttextView);

        taxTotal = (TextView) detailView.findViewById(R.id.taxtotaltextView14a);
        granTotal = (TextView) detailView.findViewById(R.id.granTotaltextView16);
        TextView footer1 = (TextView) detailView.findViewById(R.id.footerLine1textView);
        TextView footer2 = (TextView) detailView.findViewById(R.id.footerLine2textView);
        TextView footer3 = (TextView) detailView.findViewById(R.id.footerLine3textView);
        orderProductSection = (LinearLayout) detailView.findViewById(R.id.order_products_section_linearlayout);
        receiptPreview = (LinearLayout) detailView.findViewById(R.id.receiptPreviewContainer);
        deviceName.setText(String.format("%s(%s)", myPref.getEmpName(), myPref.getEmpID()));
        orderDate.setText(DateUtils.getDateAsString(new Date(), "MMM/dd/yyyy"));


        if (header[0] != null && !header[0].isEmpty()) {
            header1.setText(header[0]);
        } else {
            header1.setVisibility(View.GONE);
        }
        if (header[1] != null && !header[1].isEmpty()) {
            header2.setText(header[1]);
        } else {
            header2.setVisibility(View.GONE);
        }
        if (header[2] != null && !header[2].isEmpty()) {
            header3.setText(header[2]);
        } else {
            header3.setVisibility(View.GONE);
        }

        if (footer[0] != null && !footer[0].isEmpty()) {
            footer1.setText(footer[0]);
        } else {
            footer1.setVisibility(View.GONE);
        }
        if (footer[1] != null && !footer[1].isEmpty()) {
            footer2.setText(footer[1]);
        } else {
            footer2.setVisibility(View.GONE);
        }
        if (footer[2] != null && !footer[2].isEmpty()) {
            footer3.setText(footer[2]);
        } else {
            footer3.setVisibility(View.GONE);
        }
        return detailView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void addProductLine(String leftString, String rightString, int indentedTabs) {

        LinearLayout itemLL;
        switch (indentedTabs) {
            default:
                itemLL = (LinearLayout) inflater.inflate(R.layout.twocols_leftweight_layout_item, null, false);
                break;
            case 2:
                itemLL = (LinearLayout) inflater.inflate(R.layout.twocols_leftweight_margin1_layout_item, null, false);
                break;
            case 3:
                itemLL = (LinearLayout) inflater.inflate(R.layout.twocols_leftweight_margin2_layout_item, null, false);
                break;
        }

        TextView leftText = (TextView) itemLL.findViewById(R.id.lefttextView);
        TextView rightText = (TextView) itemLL.findViewById(R.id.righttextView);

        if (rightString == null) {
            rightText.setVisibility(View.GONE);
        } else {
            rightText.setVisibility(View.VISIBLE);
            rightText.setText(rightString);
        }

        if (leftString == null) {
            leftText.setVisibility(View.GONE);
        } else {
            leftText.setVisibility(View.VISIBLE);
            leftText.setText(leftString);
        }

        productAddonsSection.addView(itemLL);
    }


    public void setReceiptOrder(SplitedOrder splitedOrder) {
        restaurantSplitedOrder = splitedOrder;
        SplittedOrderSummary_FA orderSummaryFa = (SplittedOrderSummary_FA) getActivity();

        List<OrderProduct> products = splitedOrder.getOrderProducts();
        if (orderProductSection.getChildCount() > 0) {
            orderProductSection.removeAllViewsInLayout();
        }
        BigDecimal orderSubtotal = new BigDecimal(0);
        BigDecimal orderTaxes = new BigDecimal(0);
        BigDecimal orderGranTotal = new BigDecimal(0);
        BigDecimal itemDiscountTotal = new BigDecimal(0);
        BigDecimal globalDiscountTotal = new BigDecimal(0);
        BigDecimal tempTaxableAmount = new BigDecimal(0);
        for (OrderProduct product : products) {
            getView();
            LinearLayout productSectionLL = (LinearLayout) View.inflate(getActivity(), R.layout.receipt_product_layout_item, null);

            List<OrderProduct> addons = OrderProductsHandler.getOrderProductAddons(product.ordprod_id);

            BigDecimal qty = Global.getBigDecimalNum(product.ordprod_qty);
            orderSubtotal = orderSubtotal.add(Global.getBigDecimalNum(product.overwrite_price).multiply(qty));
            globalDiscountTotal = globalDiscountTotal.add(Global.getBigDecimalNum(product.overwrite_price).setScale(4, RoundingMode.HALF_UP)
                    .multiply(orderSummaryFa.getGlobalDiscountPercentge().setScale(6, RoundingMode.HALF_UP)));
//            orderTaxes = orderTaxes.add(Global.getBigDecimalNum(product.prod_taxValue));
            itemDiscountTotal = itemDiscountTotal.add(Global.getBigDecimalNum(product.discount_value));
//            orderGranTotal = orderGranTotal.add((Global.getBigDecimalNum(product.itemTotal))
//                    .add(Global.getBigDecimalNum(product.prod_taxValue)));
            ((TextView) productSectionLL.findViewById(R.id.productNametextView)).setText(String.format("%sx %s", product.ordprod_qty, product.ordprod_name));

            productAddonsSection = (LinearLayout) productSectionLL.findViewById(R.id.productAddonSectionLinearLayout);

            for (OrderProduct addon : addons) {
                addProductLine("- " + addon.ordprod_name,
                        Global.getCurrencyFormat(addon.overwrite_price), 3);
            }
            ((TextView) productSectionLL.findViewById(R.id.productPricetextView)).setText(Global.getCurrencyFormat(product.overwrite_price));

            ((TextView) productSectionLL.findViewById(R.id.productDiscounttextView)).setText(Global.getCurrencyFormat(product.discount_value));

            ((TextView) productSectionLL.findViewById(R.id.productTotaltextView)).setText(Global.getCurrencyFormat(Global.getBigDecimalNum(product.itemTotal)
                    .multiply(qty).toString()));

            if (product.ordprod_desc != null && !product.ordprod_desc.isEmpty()) {
                ((TextView) productSectionLL.findViewById(R.id.productDescriptiontextView)).setText(product.ordprod_desc.replace("<br/>", "\n\r"));
            } else {
                ((TextView) productSectionLL.findViewById(R.id.productDescriptiontextView)).setText("");
            }
            if (orderSummaryFa.getTax() != null) {
                TaxesCalculator taxesCalculator = new TaxesCalculator(getActivity(), product, splitedOrder.tax_id,
                        orderSummaryFa.getTax(), orderSummaryFa.getDiscount(), Global.getBigDecimalNum(splitedOrder.ord_subtotal),
                        Global.getBigDecimalNum(splitedOrder.ord_discount));
                orderTaxes = orderTaxes.add(taxesCalculator.getTaxableAmount());
            }
            orderProductSection.addView(productSectionLL);
        }

//        globalDiscountTotal = orderSubtotal.multiply(orderSummaryFa.getGlobalDiscountPercentge()).setScale(4, RoundingMode.HALF_UP);
        orderGranTotal = orderSubtotal.subtract(itemDiscountTotal).setScale(6, RoundingMode.HALF_UP)
                .subtract(globalDiscountTotal).setScale(6, RoundingMode.HALF_UP).add(orderTaxes)
                .setScale(6, RoundingMode.HALF_UP);
        splitedOrder.ord_total = orderGranTotal.toString();
        splitedOrder.gran_total = orderGranTotal.toString();
        splitedOrder.ord_subtotal = orderSubtotal.toString();
        splitedOrder.ord_taxamount = orderTaxes.toString();
        splitedOrder.ord_discount = globalDiscountTotal.toString();
        splitedOrder.ord_lineItemDiscount = itemDiscountTotal.toString();
        subtotal.setText(Global.formatDoubleStrToCurrency(orderSubtotal.toString()));
        lineItemDiscountTotal.setText(Global.formatDoubleStrToCurrency(itemDiscountTotal.toString()));
        taxTotal.setText(Global.formatDoubleStrToCurrency(orderTaxes.toString()));
        granTotal.setText(Global.formatDoubleStrToCurrency(orderGranTotal.toString()));
        orderId.setText(splitedOrder.ord_id);
        globalDiscountTextView.setText(Global.formatDoubleStrToCurrency(globalDiscountTotal.toString()));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.checkoutbutton: {
                saveHoldOrder(restaurantSplitedOrder);
                break;
            }
            case R.id.printReceiptbutton2: {
                if (Global.mainPrinterManager != null
                        && Global.mainPrinterManager.currentDevice != null) {
                    new PrintPreview().execute(receiptPreview);
                }
                break;
            }
            case R.id.printAllReceiptbutton3: {
                if (Global.mainPrinterManager != null
                        && Global.mainPrinterManager.currentDevice != null) {
                    final int[] count = {0};
                    SplittedOrderSummary_FA orderSummaryFa = (SplittedOrderSummary_FA) getActivity();
                    final SplittedOrderSummaryAdapter adapter = (SplittedOrderSummaryAdapter) orderSummaryFa.getOrderSummaryFR().getGridView().getAdapter();
                    ViewTreeObserver vto = receiptPreview.getViewTreeObserver();
                    vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            count[0]++;
                            Global.mainPrinterManager.currentDevice.printReceiptPreview(receiptPreview);
                            if (count[0] < adapter.getCount()) {
                                setReceiptOrder((SplitedOrder) adapter.getItem(count[0]));
                            } else {
                                receiptPreview.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }

                        }
                    });

                    setReceiptOrder((SplitedOrder) adapter.getItem(count[0]));
                }
            }
        }
    }


    public class PrintPreview extends AsyncTask<LinearLayout, Void, Void> {
        private ProgressDialog myProgressDialog;

        @Override
        protected void onPreExecute() {
            myProgressDialog = new ProgressDialog(getActivity());
            myProgressDialog.setMessage("Printing...");
            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myProgressDialog.setCancelable(false);
            myProgressDialog.show();
        }

        @Override
        protected Void doInBackground(LinearLayout... params) {
            Global.mainPrinterManager.currentDevice.printReceiptPreview(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void none) {
            myProgressDialog.dismiss();
        }
    }

    private void saveHoldOrder(SplitedOrder splitedOrder) {
        OrdersHandler ordersHandler = new OrdersHandler(getActivity());
        OrderTaxes_DB ordTaxesDB = new OrderTaxes_DB();
        Global global = (Global) getActivity().getApplication();
        OrderProductsHandler productsHandler = new OrderProductsHandler(getActivity());
        GenerateNewID idGen = new GenerateNewID(getActivity());
        SplittedOrderSummary_FA summaryFa = (SplittedOrderSummary_FA) getActivity();
        String nextOrderID;
        if (summaryFa.getOrderSummaryFR().getGridView().getAdapter().getCount() > 1) {
            for (OrderProduct product : splitedOrder.getOrderProducts()) {
                if (global.orderProducts.contains(product)) {
                    global.orderProducts.remove(product);
                }
            }
            if (summaryFa.splitType != SplittedOrderSummary_FA.SalesReceiptSplitTypes.SPLIT_EQUALLY) {
                global.order.ord_subtotal = Global.getBigDecimalNum(global.order.ord_subtotal)
                        .subtract(Global.getBigDecimalNum(splitedOrder.ord_subtotal)).toString();

                global.order.ord_total = Global.getBigDecimalNum(global.order.ord_total)
                        .subtract(Global.getBigDecimalNum(splitedOrder.ord_total)).toString();

                global.order.gran_total = Global.getBigDecimalNum(global.order.gran_total)
                        .subtract(Global.getBigDecimalNum(splitedOrder.gran_total)).toString();
            }
            global.order.isOnHold = "1";
            if (global.order.ord_HoldName == null || global.order.ord_HoldName.isEmpty()) {
                global.order.ord_HoldName = "Table " + global.order.assignedTable + " " + DateUtils.getDateAsString(new Date(), "MMM/dd/yy hh:mm");
            }
            global.order.processed = "10";

        }

        if (splitedOrder.getOrderProducts().size() > 0) {
            if (summaryFa.getOrderSummaryFR().getGridView().getAdapter().getCount() == 1) {
                splitedOrder.processed = "10";
                splitedOrder.isOnHold = "0";
                global.order.isOnHold = "0";
                global.order.processed = "10";
                global.order.total_lines = String.valueOf(splitedOrder.getOrderProducts().size());
                splitedOrder.ord_id = global.order.ord_id;

                if (summaryFa.splitType == SplittedOrderSummary_FA.SalesReceiptSplitTypes.SPLIT_EQUALLY) {
//                    splitedOrder.getOrderProducts().clear();
                    for (OrderSeatProduct seatProduct : summaryFa.orderSeatProducts) {
                        if (seatProduct.rowType == OrderProductListAdapter.RowType.TYPE_ITEM && seatProduct.orderProduct != null) {
                            splitedOrder.getOrderProducts().add(seatProduct.orderProduct);
                        }
                    }
                    splitedOrder.total_lines = String.valueOf(splitedOrder.getOrderProducts().size());
                    splitedOrder.syncOrderProductIds();
                    ordersHandler.insert(global.order);
                } else {
                    splitedOrder.total_lines = String.valueOf(splitedOrder.getOrderProducts().size());
                    splitedOrder.syncOrderProductIds();
                    ordersHandler.insert(splitedOrder);
                }
                global.encodedImage = "";
                productsHandler.insert(splitedOrder.getOrderProducts());
                if (global.listOrderTaxes != null && global.listOrderTaxes.size() > 0) {
                    ordTaxesDB.insert(global.listOrderTaxes, global.order.ord_id);
                }


                DBManager dbManager = new DBManager(getActivity());
                dbManager.synchSendOrdersOnHold(false, true);
            } else if (summaryFa.splitType == SplittedOrderSummary_FA.SalesReceiptSplitTypes.SPLIT_EQUALLY) {
                splitedOrder.ord_id = global.order.ord_id;
                splitedOrder.syncOrderProductIds();
            } else {

                nextOrderID = idGen.getNextID(GenerateNewID.IdType.ORDER_ID);
                splitedOrder.ord_id = nextOrderID;
                splitedOrder.processed = "10";
                splitedOrder.isOnHold = "1";
                splitedOrder.syncOrderProductIds();
                ordersHandler.insert(splitedOrder);
                productsHandler.insert(splitedOrder.getOrderProducts());
                ordTaxesDB.insert(global.listOrderTaxes, splitedOrder.ord_id);
            }

            Receipt_FR.updateLocalInventory(getActivity(), splitedOrder.getOrderProducts(), false);
            if (Global.getBigDecimalNum(splitedOrder.gran_total).compareTo(new BigDecimal(0)) != -1) {
                Receipt_FR.updateLocalInventory(getActivity(), splitedOrder.getOrderProducts(), false);
                isSalesReceipt(splitedOrder);
            }
        }
    }


    private void isSalesReceipt(SplitedOrder order) {
        Intent intent = new Intent(getActivity(), SelectPayMethod_FA.class);
        intent.putExtra("typeOfProcedure", Global.TransactionType.SALE_RECEIPT);
        intent.putExtra("salesreceipt", true);
        intent.putExtra("amount", Global.getRoundBigDecimal(Global.getBigDecimalNum(order.gran_total)
                .compareTo(new BigDecimal(0)) == -1 ? Global.getBigDecimalNum(order.gran_total)
                .negate() : Global.getBigDecimalNum(order.gran_total)));
        intent.putExtra("paid", "0.00");
        intent.putExtra("is_receipt", true);
        intent.putExtra("job_id", order.ord_id);
        intent.putExtra("ord_subtotal", order.ord_subtotal);
        intent.putExtra("ord_taxID", order.tax_id);
        intent.putExtra("ord_type", Global.OrderType.SALES_RECEIPT);
        intent.putExtra("ord_email", "");

        if (myPref.isCustSelected()) {
            intent.putExtra("cust_id", myPref.getCustID());
            intent.putExtra("custidkey", myPref.getCustIDKey());
        }
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SplittedOrderSummary_FA summaryFa = (SplittedOrderSummary_FA) getActivity();
        summaryFa.checkoutCount++;
        if (resultCode == SplittedOrderSummary_FA.NavigationResult.PAYMENT_SELECTION_VOID.getCode()) {
            summaryFa.voidTransaction(false);
        } else if (summaryFa.splitType == SplittedOrderSummary_FA.SalesReceiptSplitTypes.SPLIT_EQUALLY
                && resultCode != SplittedOrderSummary_FA.NavigationResult.BACK_SELECT_PAYMENT.getCode()) {
            removeCheckoutOrder(summaryFa);
            summaryFa.findViewById(R.id.splitTypesSpinner).setEnabled(false);
            if (summaryFa.getOrderSummaryFR().getGridView().getAdapter().getCount() == 0) {
                getActivity().setResult(-1);
                getActivity().finish();
            } else {
                summaryFa.getOrderDetailsFR().setReceiptOrder((SplitedOrder) summaryFa.getOrderSummaryFR().getGridView().getAdapter().getItem(0));
            }
        } else if (resultCode == SplittedOrderSummary_FA.NavigationResult.PAYMENT_COMPLETED.getCode()) {
            removeCheckoutOrder(summaryFa);
            if (summaryFa.getOrderSummaryFR().getGridView().getAdapter().getCount() == 0) {
                getActivity().setResult(-1);
                getActivity().finish();
            } else {
                summaryFa.getOrderDetailsFR().setReceiptOrder((SplitedOrder) summaryFa.getOrderSummaryFR().getGridView().getAdapter().getItem(0));
            }
        } else {//Rollback order checkout

            Global global = (Global) getActivity().getApplication();
            for (OrderProduct product : restaurantSplitedOrder.getOrderProducts()) {
                product.ord_id = global.order.ord_id;
                if (summaryFa.splitType != SplittedOrderSummary_FA.SalesReceiptSplitTypes.SPLIT_EQUALLY) {
                    global.order.ord_subtotal = Global.getBigDecimalNum(global.order.ord_subtotal)
                            .add(Global.getBigDecimalNum(restaurantSplitedOrder.ord_subtotal)).toString();

                    global.order.ord_total = Global.getBigDecimalNum(global.order.ord_total)
                            .add(Global.getBigDecimalNum(restaurantSplitedOrder.ord_total)).toString();

                    global.order.gran_total = Global.getBigDecimalNum(global.order.gran_total)
                            .add(Global.getBigDecimalNum(restaurantSplitedOrder.gran_total)).toString();
                }
            }

            OrdersHandler ordersHandler = new OrdersHandler(getActivity());
            OrderTaxes_DB orderTaxesDB = new OrderTaxes_DB();
            OrderProductsHandler productsHandler = new OrderProductsHandler(getActivity());
            ordersHandler.insert(global.order);
            productsHandler.insert(restaurantSplitedOrder.getOrderProducts());
            orderTaxesDB.insert(global.listOrderTaxes, global.order.ord_id);
            ordersHandler.deleteOrder(restaurantSplitedOrder.ord_id);

        }
    }

    private void removeCheckoutOrder(SplittedOrderSummary_FA summaryFa) {
        SplittedOrderSummaryAdapter adapter = (SplittedOrderSummaryAdapter) summaryFa.getOrderSummaryFR().getGridView().getAdapter();
        adapter.removeOrder(restaurantSplitedOrder);
        removeTicket(restaurantSplitedOrder);
        if (!adapter.isEmpty()) {
            adapter.setSelectedIndex(0);
            restaurantSplitedOrder = (SplitedOrder) adapter.getItem(0);
        }
    }

    private void removeTicket(SplitedOrder splitedOrder) {
        SplittedOrderSummary_FA summaryFa = (SplittedOrderSummary_FA) getActivity();
        List<OrderSeatProduct> seatProducts = new ArrayList<OrderSeatProduct>(summaryFa.orderSeatProducts);

        List<OrderProduct> products = splitedOrder.getOrderProducts();
        for (OrderProduct product : products) {
            for (OrderSeatProduct seatProduct : seatProducts) {
                if (seatProduct.rowType == OrderProductListAdapter.RowType.TYPE_ITEM &&
                        seatProduct.orderProduct.ordprod_id.equalsIgnoreCase(product.ordprod_id)) {
                    summaryFa.orderSeatProducts.remove(seatProduct);
                }
            }
        }
    }
}