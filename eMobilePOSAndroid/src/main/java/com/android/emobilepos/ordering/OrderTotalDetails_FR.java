package com.android.emobilepos.ordering;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.dao.AssignEmployeeDAO;
import com.android.dao.MixMatchDAO;
import com.android.database.ProductsHandler;
import com.android.database.TaxesGroupHandler;
import com.android.database.TaxesHandler;
import com.android.emobilepos.R;
import com.android.emobilepos.models.DataTaxes;
import com.android.emobilepos.models.Discount;
import com.android.emobilepos.models.MixAndMatchDiscount;
import com.android.emobilepos.models.MixMatchProductGroup;
import com.android.emobilepos.models.MixMatchXYZProduct;
import com.android.emobilepos.models.Tax;
import com.android.emobilepos.models.orders.OrderProduct;
import com.android.emobilepos.models.orders.OrderTotalDetails;
import com.android.emobilepos.models.realms.AssignEmployee;
import com.android.emobilepos.models.realms.MixMatch;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.crashlytics.android.Crashlytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.RealmResults;
import io.realm.Sort;
import util.StringUtil;

public class OrderTotalDetails_FR extends Fragment implements Receipt_FR.RecalculateCallback {
    public static String discountID = "", taxID = "";
    public static BigDecimal tax_amount = new BigDecimal("0"), discount_amount = new BigDecimal("0"),
            discount_rate = new BigDecimal("0"), discountable_sub_total = new BigDecimal("0"),
            sub_total = new BigDecimal("0"), gran_total = new BigDecimal("0");
    public static BigDecimal itemsDiscountTotal = new BigDecimal(0);
    private static ReCalculate recalculateTask;
    private Spinner taxSpinner, discountSpinner;
    private List<Tax> taxList;
    private List<Discount> discountList;
    private int taxSelected;
    private int discountSelected;
    private EditText globalDiscount, globalTax, subTotal;
    private Button discountBtn;
    private TextView granTotal;
    private List<HashMap<String, String>> listMapTaxes = new ArrayList<>();
    private Activity activity;
    private MyPreferences myPref;
    private TaxesHandler taxHandler;
    private TaxesGroupHandler taxGroupHandler;
    private AssignEmployee assignEmployee;
    private boolean isToGo;
    boolean validPassword = false;

    public static OrderTotalDetails_FR init(int val) {
        OrderTotalDetails_FR frag = new OrderTotalDetails_FR();

        Bundle args = new Bundle();
        args.putInt("val", val);
        frag.setArguments(args);
        return frag;
    }

    private static void calculateMixAndMatch(List<OrderProduct> orderProducts, boolean isGroupBySKU) {
        List<OrderProduct> noMixMatchProducts = new ArrayList<>();
        HashMap<String, MixMatchProductGroup> mixMatchProductGroupHashMap = new HashMap<>();
        for (OrderProduct product : orderProducts) {
            BigDecimal overwrite = product.getOverwrite_price();
            product.resetMixMatch();

            if (TextUtils.isEmpty(product.getPricesXGroupid()) || product.isVoid()) {
                noMixMatchProducts.add(product);
            } else if (overwrite != null || !TextUtils.isEmpty(product.getDiscount_id())) {
                noMixMatchProducts.add(product);
            } else {
                product.setMixAndMatchDiscounts(new ArrayList<MixAndMatchDiscount>());
                if (product.getMixMatchOriginalPrice() == null || product.getMixMatchOriginalPrice().compareTo(new BigDecimal(0)) == 0) {
                    product.setMixMatchOriginalPrice(Global.getBigDecimalNum(product.getProd_price()));
                }
                MixMatchProductGroup mixMatchProductGroup = mixMatchProductGroupHashMap.get(product.getPricesXGroupid());
                if (mixMatchProductGroup != null) {
                    mixMatchProductGroup.getOrderProducts().add(product);
                    mixMatchProductGroup.setQuantity(mixMatchProductGroup.getQuantity() + Double.valueOf(product.getOrdprod_qty()).intValue());
                } else {
                    mixMatchProductGroup = new MixMatchProductGroup();
                    mixMatchProductGroup.setOrderProducts(new ArrayList<OrderProduct>());
                    mixMatchProductGroup.getOrderProducts().add(product);
                    mixMatchProductGroup.setGroupId(product.getPricesXGroupid());
                    mixMatchProductGroup.setPriceLevelId(StringUtil.nullStringToEmpty(product.getPricelevel_id()));
                    mixMatchProductGroup.setQuantity(Double.valueOf(product.getOrdprod_qty()).intValue());
                    mixMatchProductGroupHashMap.put(product.getPricesXGroupid(), mixMatchProductGroup);
                }
            }
        }
        orderProducts.clear();
        for (Map.Entry<String, MixMatchProductGroup> mixMatchProductGroupEntry : mixMatchProductGroupHashMap.entrySet()) {
            MixMatchProductGroup group = mixMatchProductGroupEntry.getValue();
            RealmResults<MixMatch> mixMatches = MixMatchDAO.getDiscountsBygroupId(group);
            if (!mixMatches.isEmpty()) {
                MixMatch mixMatch = mixMatches.get(0);
                int mixMatchType = mixMatch.getMixMatchType();
                if (mixMatchType == 1) {
                    mixMatches = mixMatches.sort("qty", Sort.DESCENDING);
                    orderProducts.addAll(applyMixMatch(group, mixMatches));
                } else {
                    if (mixMatches.size() == 2) {
                        mixMatches = mixMatches.sort("xyzSequence", Sort.ASCENDING);
                        orderProducts.addAll(applyXYZMixMatchToGroup(group, mixMatches, isGroupBySKU));
                    } else {
                        orderProducts.addAll(group.getOrderProducts());
                    }
                }
            } else if (!group.getOrderProducts().isEmpty()) {
                orderProducts.addAll(group.getOrderProducts());
            }
            mixMatches.getRealm().close();
        }
        orderProducts.addAll(noMixMatchProducts);
    }

    private static List<OrderProduct> applyXYZMixMatchToGroup(MixMatchProductGroup group, RealmResults<MixMatch> mixMatches, boolean isGroupBySKU) {
        List<OrderProduct> orderProducts = new ArrayList<>();
        MixMatch mixMatch1 = mixMatches.get(0);
        MixMatch mixMatch2 = mixMatches.get(1);
        int qtyRequired = mixMatch1.getQty();
        int qtyDiscounted = mixMatch2.getQty();
        Double amount = mixMatch2.getPrice();
        boolean isPercent = mixMatch2.isPercent();
        if (group.getQuantity() < qtyRequired) {
            return group.getOrderProducts();
        }

        int qtyAtRegularPrice;
        int groupQty = group.getQuantity();
        int completeGroupSize = qtyRequired + qtyDiscounted;
        int numberOfCompletedGroups = groupQty / completeGroupSize;
        int remainingItems = groupQty % completeGroupSize;

        qtyAtRegularPrice = numberOfCompletedGroups * qtyRequired;

        if (remainingItems > qtyRequired) {
            qtyAtRegularPrice += qtyRequired;
        } else {
            qtyAtRegularPrice += remainingItems;
        }
        List<MixMatchXYZProduct> mixMatchXYZProducts = new ArrayList<>();

        for (OrderProduct product : group.getOrderProducts()) {
            if (mixMatchXYZProducts.contains(product.getProd_id())) {
                int indexOf = mixMatchXYZProducts.indexOf(product.getProd_id());
                MixMatchXYZProduct mmxyz = mixMatchXYZProducts.get(indexOf);
                mmxyz.setQuantity(mmxyz.getQuantity() + Double.valueOf(product.getOrdprod_qty()).intValue());
                mmxyz.getOrderProducts().add(product);
                mmxyz.setPrice(product.getMixMatchOriginalPrice());
                mixMatchXYZProducts.set(indexOf, mmxyz);
            } else {
                MixMatchXYZProduct mmxyz = new MixMatchXYZProduct();
                mmxyz.setProductId(product.getProd_id());
                mmxyz.setQuantity(Double.valueOf(product.getOrdprod_qty()).intValue());
                mmxyz.getOrderProducts().add(product);
                mmxyz.setPrice(product.getMixMatchOriginalPrice());
                mixMatchXYZProducts.add(mmxyz);
            }
        }
        Collections.sort(mixMatchXYZProducts, new Comparator<MixMatchXYZProduct>() {
            @Override
            public int compare(MixMatchXYZProduct a, MixMatchXYZProduct b) {
                return b.getPrice().compareTo(a.getPrice());
            }
        });
        orderProducts.clear();


        for (MixMatchXYZProduct xyzProduct : mixMatchXYZProducts) {
            int prodQty = xyzProduct.getQuantity();
            if (prodQty <= qtyAtRegularPrice) {
                if (isGroupBySKU) {
                    OrderProduct orderProduct = null;
                    try {
                        orderProduct = (OrderProduct) xyzProduct.getOrderProducts().get(0).clone();
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                        Crashlytics.logException(e);
                    }
                    orderProduct.setProd_price(String.valueOf(xyzProduct.getPrice()));
                    orderProduct.setOrdprod_qty(String.valueOf(prodQty));
                    orderProduct.setMixMatchQtyApplied(prodQty);
                    orderProduct.setItemTotal(String.valueOf(xyzProduct.getPrice().multiply(Global.getBigDecimalNum(orderProduct.getOrdprod_qty()))));
                    orderProducts.add(orderProduct);
                } else {
                    for (OrderProduct orderProduct : xyzProduct.getOrderProducts()) {
                        OrderProduct clone = null;
                        try {
                            clone = (OrderProduct) orderProduct.clone();
                        } catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                            Crashlytics.logException(e);
                        }
                        clone.setOrdprod_qty("1");
                        clone.setProd_price(String.valueOf(xyzProduct.getPrice()));
                        clone.setMixMatchQtyApplied(1);
                        clone.setItemTotal(clone.getProd_price());
                        orderProducts.add(clone);
                    }
                }
                qtyAtRegularPrice -= prodQty;
            } else {
                int regularPriced = qtyAtRegularPrice;
                int discountPriced = prodQty - qtyAtRegularPrice;
                if (regularPriced > 0) {
                    if (isGroupBySKU) {
                        OrderProduct orderProduct = null;
                        try {
                            orderProduct = (OrderProduct) xyzProduct.getOrderProducts().get(0).clone();
                        } catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                            Crashlytics.logException(e);
                        }
                        orderProduct.setProd_price(String.valueOf(xyzProduct.getPrice()));
                        orderProduct.setOrdprod_qty(String.valueOf(regularPriced));
                        orderProduct.setMixMatchQtyApplied(regularPriced);
                        orderProduct.setItemTotal(String.valueOf(xyzProduct.getPrice()
                                .multiply(Global.getBigDecimalNum(orderProduct.getOrdprod_qty()))));

                        orderProducts.add(orderProduct);
                    } else {
                        for (int i = 0; i < regularPriced; i++) {
                            try {
                                OrderProduct clone = (OrderProduct) xyzProduct.getOrderProducts().get(0).clone();
                                clone.setProd_price(String.valueOf(xyzProduct.getPrice()));
                                clone.setOrdprod_qty("1");
                                clone.setMixMatchQtyApplied(1);
                                clone.setItemTotal(clone.getProd_price());
                                orderProducts.add(clone);
                            } catch (CloneNotSupportedException e) {
                                e.printStackTrace();
                                Crashlytics.logException(e);
                            }
                        }

                    }
                    qtyAtRegularPrice -= regularPriced;
                }
                if (discountPriced > 0) {
                    if (isGroupBySKU) {
                        OrderProduct orderProduct = null;
                        try {
                            orderProduct = (OrderProduct) xyzProduct.getOrderProducts().get(0).clone();
                        } catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                            Crashlytics.logException(e);
                        }
                        orderProduct.setOrdprod_qty(String.valueOf(discountPriced));
                        orderProduct.setMixMatchQtyApplied(discountPriced);
                        BigDecimal discountPrice;
                        if (isPercent) {
                            BigDecimal hundred = new BigDecimal(100);
                            BigDecimal percent = (hundred.subtract(new BigDecimal(amount))).divide(hundred);
                            discountPrice = xyzProduct.getPrice().multiply(percent);
                        } else {
                            discountPrice = BigDecimal.valueOf(amount);
                        }
                        orderProduct.setProd_price(String.valueOf(discountPrice));
                        orderProduct.setItemTotal(String.valueOf(Global.getBigDecimalNum(orderProduct.getProd_price()).multiply(Global.getBigDecimalNum(orderProduct.getOrdprod_qty()))));
                        orderProducts.add(orderProduct);
                    } else {
                        for (int i = 0; i < discountPriced; i++) {
                            try {
                                OrderProduct clone = (OrderProduct) xyzProduct.getOrderProducts().get(0).clone();
                                clone.setOrdprod_qty("1");
                                clone.setMixMatchQtyApplied(1);

                                BigDecimal discountPrice;
                                if (isPercent) {
                                    BigDecimal hundred = new BigDecimal(100);
                                    BigDecimal percent = (hundred.subtract(new BigDecimal(amount))).divide(hundred);
                                    discountPrice = xyzProduct.getPrice().multiply(percent);
                                } else {
                                    discountPrice = BigDecimal.valueOf(amount);
                                }
                                clone.setProd_price(String.valueOf(discountPrice));
                                clone.setItemTotal(clone.getProd_price());
                                orderProducts.add(clone);
                            } catch (CloneNotSupportedException e) {
                                e.printStackTrace();
                                Crashlytics.logException(e);
                            }
                        }

                    }
                }

            }
        }
        return orderProducts;
    }

    private static List<OrderProduct> applyMixMatch(MixMatchProductGroup group, RealmResults<MixMatch> mixMatches) {
        MixMatch firstMixMatch = mixMatches.get(0);
        if (group.getQuantity() >= firstMixMatch.getQty() && firstMixMatch.isDiscountOddsItems()) {
            for (OrderProduct product : group.getOrderProducts()) {
                if (firstMixMatch.isFixed()) {
                    product.setProd_price(String.valueOf(firstMixMatch.getPrice()));
                } else {
                    double percent = (100 - firstMixMatch.getPrice()) / 100;
                    String prod_price = Global.getRoundBigDecimal(product.getMixMatchOriginalPrice()
                            .multiply(new BigDecimal(percent))).toString();
                    product.setPrices(prod_price, product.getOrdprod_qty());
                }
            }
        } else {
            int itemsRemaining = group.getQuantity();
            for (MixMatch mixMatch : mixMatches) {
                int volumeQty = mixMatch.getQty();
                if (volumeQty <= itemsRemaining) {
                    int itemsToDiscount = volumeQty * (itemsRemaining / volumeQty);
                    for (OrderProduct product : group.getOrderProducts()) {
                        if (product.getMixMatchQtyApplied() < Integer.parseInt(product.getOrdprod_qty())) {
                            int qtyRemainning = Integer.parseInt(product.getOrdprod_qty()) - product.getMixMatchQtyApplied();
                            if (qtyRemainning < itemsToDiscount) {
                                MixAndMatchDiscount mixAndMatchDiscount = new MixAndMatchDiscount();
                                mixAndMatchDiscount.setQty(qtyRemainning);
                                mixAndMatchDiscount.setMixMatch(mixMatch);
                                product.getMixAndMatchDiscounts().add(mixAndMatchDiscount);
                                product.setMixMatchQtyApplied(product.getMixMatchQtyApplied() + qtyRemainning);
                                itemsRemaining -= qtyRemainning;
                                itemsToDiscount -= qtyRemainning;
                            } else {
                                qtyRemainning = itemsToDiscount;
                                MixAndMatchDiscount mixAndMatchDiscount = new MixAndMatchDiscount();
                                mixAndMatchDiscount.setQty(qtyRemainning);
                                mixAndMatchDiscount.setMixMatch(mixMatch);
                                product.getMixAndMatchDiscounts().add(mixAndMatchDiscount);
                                product.setMixMatchQtyApplied(product.getMixMatchQtyApplied() + qtyRemainning);
                                itemsRemaining -= qtyRemainning;
                                itemsToDiscount = 0;
                            }
                            if (itemsRemaining == 0 || itemsToDiscount == 0) {
                                break;
                            }
                        }
                    }
                }
            }
            for (OrderProduct product : group.getOrderProducts()) {
                BigDecimal lineTotal = new BigDecimal(0);
                for (MixAndMatchDiscount discount : product.getMixAndMatchDiscounts()) {
                    MixMatch mixMatch = discount.getMixMatch();
                    BigDecimal discountSplitAmount;
                    if (mixMatch.isFixed()) {
                        discountSplitAmount = new BigDecimal(mixMatch.getPrice())
                                .multiply(BigDecimal.valueOf(discount.getQty()));
                    } else {
                        BigDecimal percent = new BigDecimal(100 - mixMatch.getPrice()).divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
                        discountSplitAmount = product.getMixMatchOriginalPrice()
                                .multiply(percent)
                                .multiply(BigDecimal.valueOf(discount.getQty()));
                    }
                    lineTotal = lineTotal.add(discountSplitAmount);
                }
                if (product.getMixMatchQtyApplied() != Integer.parseInt(product.getOrdprod_qty())) {
                    int qtyNotDicounted = Integer.parseInt(product.getOrdprod_qty()) - product.getMixMatchQtyApplied();
                    BigDecimal amountNotDiscounted = new BigDecimal(qtyNotDicounted)
                            .multiply(product.getMixMatchOriginalPrice());
                    lineTotal = lineTotal.add(amountNotDiscounted);
                }
                String prod_price = lineTotal.divide(new BigDecimal(product.getOrdprod_qty()), 2, RoundingMode.HALF_UP).toString();
                product.setPrices(prod_price, product.getOrdprod_qty());
            }
        }
        return group.getOrderProducts();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.order_total_details_layout, container, false);
        assignEmployee = AssignEmployeeDAO.getAssignEmployee();
        isToGo = ((OrderingMain_FA) getActivity()).isToGo;
        taxSelected = 0;
        discountSelected = 0;
        subTotal = view.findViewById(R.id.subtotalField);
        taxSpinner = view.findViewById(R.id.globalTaxSpinner);
        globalTax = view.findViewById(R.id.globalTaxField);
        discountSpinner = view.findViewById(R.id.globalDiscountSpinner);
        globalDiscount = view.findViewById(R.id.globalDiscountField);
        discountBtn     = view.findViewById(R.id.discountBtn);
        if(discountBtn != null)
            discountBtn.setEnabled(false);
        granTotal = view.findViewById(R.id.grandTotalValue);
        activity = getActivity();
        myPref = new MyPreferences(activity);
        discountBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Check if manager password is required from preferences
                        if(myPref.isManagerPasswordRequiredForOpenDiscount()){
                            askForManagerPassDlg();
                        }else{
                            showDiscountDlog();
                        }
                    }
                }
        );
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initSpinners();
    }

    public void initSpinners() {
        if (Global.isActivityDestroyed(activity)) {
            return;
        }
        taxSelected = 0;
        listMapTaxes = new ArrayList<>();
        List<String> taxes = new ArrayList<>();
        List<String> discount = new ArrayList<>();
        String custTaxCode;
        if (myPref != null && myPref.isCustSelected()) {
            custTaxCode = myPref.getCustTaxCode();
            if (TextUtils.isEmpty(custTaxCode)) {
                custTaxCode = AssignEmployeeDAO.getAssignEmployee().getTaxDefault();
            }
        } else if (Global.isFromOnHold && !TextUtils.isEmpty(Global.taxID))
            custTaxCode = Global.taxID;
        else {
            custTaxCode = assignEmployee != null ? assignEmployee.getTaxDefault() : "";
        }
        taxes.add("Global Tax");
        discount.add("Global Discount");

        taxHandler = new TaxesHandler(activity);
        taxGroupHandler = new TaxesGroupHandler(activity);
        taxList = taxHandler.getProductTaxes(myPref.getPreferences(MyPreferences.pref_show_only_group_taxes));
        ProductsHandler handler2 = new ProductsHandler(activity);
        discountList = handler2.getDiscounts();
        int size = taxList.size();
        int size2 = discountList.size();
        boolean custTaxWasFound = false;
        int mSize = size;
        if (size < size2)
            mSize = size2;
        for (int i = 0; i < mSize; i++) {
            if (i < size) {
                taxes.add(taxList.get(i).getTaxName());
                if (!TextUtils.isEmpty(custTaxCode) && custTaxCode.equals(taxList.get(i).getTaxId())) {
                    taxSelected = i + 1;
                    custTaxWasFound = true;
                }
            }
            if (i < size2)
                discount.add(discountList.get(i).getProductName());
        }

        if (!custTaxWasFound) {
            taxSelected = 0;
            Global.taxID = "";

        }
        List<String[]> taxArr = new ArrayList<>();
        for (Tax tax : taxList) {
            String[] arr = new String[5];
            arr[0] = tax.getTaxName();
            arr[1] = tax.getTaxId();
            arr[2] = tax.getTaxRate();
            arr[3] = tax.getTaxType();
            taxArr.add(arr);
        }
        MySpinnerAdapter taxAdapter = new MySpinnerAdapter(activity, android.R.layout.simple_spinner_item, taxes, taxArr, true);
        List<String[]> discountArr = new ArrayList<>();
        for (Discount disc : discountList) {
            String[] arr = new String[5];
            arr[0] = disc.getProductName();
            arr[1] = disc.getProductDiscountType();
            arr[2] = disc.getProductPrice();
            arr[3] = disc.getTaxCodeIsTaxable();
            arr[4] = disc.getProductId();
            discountArr.add(arr);
        }
        MySpinnerAdapter discountAdapter = new MySpinnerAdapter(activity, android.R.layout.simple_spinner_item, discount, discountArr,
                false);

        taxSpinner.setAdapter(taxAdapter);
        taxSpinner.setOnItemSelectedListener(setSpinnerListener(false));
        taxSpinner.setSelection(taxSelected, false);

        discountSpinner.setAdapter(discountAdapter);
        discountSpinner.setOnItemSelectedListener(setSpinnerListener(true));
    }

    private OnItemSelectedListener setSpinnerListener(final boolean isDiscount) {
        return new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                if (isDiscount) {
                    discountSelected = pos;
                    setDiscountValue(pos);
                } else {
                    taxSelected = pos;
                    setTaxValue(pos);
                }
                if (getOrderingMainFa().global.order != null && getOrderingMainFa().global.order.getOrderProducts() != null) {
                    reCalculate(getOrderingMainFa().global.order.getOrderProducts());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                if (isDiscount)
                    discountSelected = 0;
                else
                    taxSelected = 0;
            }

        };
    }

    public void setTaxValue(int position) {
        if (position == 0) {
            tax_amount = new BigDecimal("0");
            Global.taxAmount = new BigDecimal("0");
            taxID = "";
        } else {

            taxID = taxList.get(taxSelected - 1).getTaxId();
            Global.taxAmount = Global.getBigDecimalNum(taxList.get(taxSelected - 1).getTaxRate());
            if (!myPref.isRetailTaxes()) {
                listMapTaxes = taxHandler.getTaxDetails(taxID, "");
                if (listMapTaxes.size() > 0 && listMapTaxes.get(0).get("tax_type").equals("G")) {
                    listMapTaxes = taxGroupHandler.getIndividualTaxes(listMapTaxes.get(0).get("tax_id"),
                            listMapTaxes.get(0).get("tax_code_id"));
                    setupTaxesHolder();
                }
            } else {
                listMapTaxes.clear();
                HashMap<String, String> mapTax = new HashMap<>();
                mapTax.put("tax_id", taxID);
                mapTax.put("tax_name", taxList.get(taxSelected - 1).getTaxName());
                mapTax.put("tax_rate", taxList.get(taxSelected - 1).getTaxRate());
                listMapTaxes.add(mapTax);
            }
        }
        taxSelected = position;
        setupTaxesHolder();
        Global.taxPosition = position;
        Global.taxID = taxID;
    }

    public void setDiscountValue(int position) {
        try{
            DecimalFormat frmt = new DecimalFormat("0.00");
            if (position == 0) {
                discountBtn.setEnabled(false);
                if (getOrderingMainFa().global.order == null || getOrderingMainFa().global.order.ord_discount_id.isEmpty()) {
                    discount_rate = new BigDecimal("0");
                    discount_amount = new BigDecimal("0");
                    discountID = "";
                } else {
                    discountSelected = ((MySpinnerAdapter) discountSpinner.getAdapter()).getDiscountIdPosition(getOrderingMainFa().global.order.ord_discount_id) + 1;
                    discountID = discountList.get(discountSelected - 1).getProductId();
                    if (discountList.get(discountSelected - 1).getProductDiscountType().equals("Fixed")) {
                        discount_rate = Global.getBigDecimalNum(discountList.get(discountSelected - 1).getProductPrice());
                        discount_amount = Global.getBigDecimalNum(discountList.get(discountSelected - 1).getProductPrice());
                    } else {
                        discount_rate = Global.getBigDecimalNum(discountList.get(discountSelected - 1).getProductPrice())
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        BigDecimal total = discountable_sub_total.subtract(itemsDiscountTotal);
                        discount_amount = total.multiply(discount_rate).setScale(2, RoundingMode.HALF_UP);
                    }
                }
            } else if (discountList != null && discountSelected > 0) {
                discountBtn.setEnabled(true);
                discountID = discountList.get(discountSelected - 1).getProductId();
                if (discountList.get(discountSelected - 1).getProductDiscountType().equals("Fixed")) {
                    discount_rate = Global.getBigDecimalNum(discountList.get(discountSelected - 1).getProductPrice());
                    discount_amount = Global.getBigDecimalNum(discountList.get(discountSelected - 1).getProductPrice());

                } else {
                    discount_rate = Global.getBigDecimalNum(discountList.get(discountSelected - 1).getProductPrice())
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    BigDecimal total = discountable_sub_total.subtract(itemsDiscountTotal);
                    discount_amount = total.multiply(discount_rate).setScale(2, RoundingMode.HALF_UP);
                }
            }
            globalDiscount.setText(Global.getCurrencyFormat(frmt.format(discount_amount)));
            Global.discountPosition = position;
            Global.discountAmount = discount_amount;
        }catch (Exception x){
            x.printStackTrace();
        }
    }

    private OrderingMain_FA getOrderingMainFa() {
        return (OrderingMain_FA) getActivity();
    }

    private void setupTaxesHolder() {
        int size = listMapTaxes.size();
        if (getActivity() != null) {
            getOrderingMainFa().setListOrderTaxes(new ArrayList<DataTaxes>());
        }
        DataTaxes tempTaxes;
        for (int i = 0; i < size; i++) {
            tempTaxes = new DataTaxes();
            tempTaxes.setTax_name(listMapTaxes.get(i).get("tax_name"));
            tempTaxes.setOrd_id("");
            tempTaxes.setTax_amount("0");
            tempTaxes.setTax_rate(listMapTaxes.get(i).get("tax_rate"));
            getOrderingMainFa().getListOrderTaxes().add(tempTaxes);
        }
    }

    public synchronized void reCalculate(List<OrderProduct> orderProducts) {
        if (getOrderingMainFa().global == null) {
            return;
        }
        if (getOrderingMainFa() != null) {
            recalculateTask = new ReCalculate(getActivity());
            recalculateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, orderProducts);
        }
    }

    @Override
    public void recalculateTotal() {
        if (getOrderingMainFa() != null) {
            reCalculate(getOrderingMainFa().global.order.getOrderProducts());
        }
    }

    private static class ReCalculate extends AsyncTask<List<OrderProduct>, Void, OrderTotalDetails> {

        MyPreferences myPref;
        private OrderingMain_FA activity;

        public ReCalculate(Activity activity) {
            myPref = new MyPreferences(activity);
            this.activity = (OrderingMain_FA) activity;
        }

        @Override
        protected void onPreExecute() {
            Global.lockOrientation(activity);
        }

        @Override
        protected OrderTotalDetails doInBackground(List<OrderProduct>... params) {
            OrderTotalDetails totalDetails = null;
            try {
                List<OrderProduct> orderProducts = params[0];
                if (myPref.isMixAnMatch() && orderProducts != null && !orderProducts.isEmpty()) {
                    boolean isGroupBySKU = myPref.isGroupReceiptBySku(activity.isToGo);
                    calculateMixAndMatch(orderProducts, isGroupBySKU);
                }
                Discount discount = activity.getLeftFragment().orderTotalDetailsFr.discountSelected > 0 ?
                        activity.getLeftFragment().orderTotalDetailsFr.discountList.get(activity.getLeftFragment().orderTotalDetailsFr.discountSelected - 1) : null;
                activity.global.order.setRetailTaxes(myPref.isRetailTaxes());
                activity.global.order.ord_globalDiscount = String.valueOf(discount_amount);
                activity.global.order.setListOrderTaxes(activity.getListOrderTaxes());
                Tax tax = activity.getLeftFragment().orderTotalDetailsFr.taxSelected > 0 ?
                        activity.getLeftFragment().orderTotalDetailsFr.taxList.get(activity.getLeftFragment().orderTotalDetailsFr.taxSelected - 1) : null;
                if (myPref.isRetailTaxes()) {
                    activity.global.order.setRetailTax(activity, taxID);

                }
                totalDetails = activity.global.order.getOrderTotalDetails(discount, tax, activity.getLeftFragment().orderTotalDetailsFr.assignEmployee.isVAT(), activity);
                gran_total = Global.getRoundBigDecimal(totalDetails.getGranTotal(), 2);
                sub_total = totalDetails.getSubtotal();
                tax_amount = Global.getRoundBigDecimal(totalDetails.getTax(), 2);
                discount_amount = totalDetails.getGlobalDiscount();
            } catch (Exception e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            }
            return totalDetails;
        }

        @Override
        protected synchronized void onPostExecute(OrderTotalDetails totalDetails) {
            super.onPostExecute(totalDetails);
            if (totalDetails != null) {
                Global.loyaltyCharge = String.valueOf(totalDetails.getPointsSubTotal());
                Global.loyaltyPointsAvailable = String.valueOf(totalDetails.getPointsAvailable());
                activity.getLeftFragment().orderTotalDetailsFr.subTotal.setText(Global.getCurrencyFrmt(String.valueOf(sub_total)));
                activity.getLeftFragment().orderTotalDetailsFr.granTotal.setText(Global.getCurrencyFrmt(String.valueOf(gran_total)));
                activity.getLeftFragment().orderTotalDetailsFr.globalTax.setText(Global.getCurrencyFrmt(String.valueOf(tax_amount)));
                activity.getLeftFragment().orderTotalDetailsFr.globalDiscount.setText(Global.getCurrencyFrmt(String.valueOf(discount_amount)));
                if (activity.getLoyaltyFragment() != null) {
                    activity.getLoyaltyFragment().recalculatePoints(String.valueOf(totalDetails.getPointsSubTotal()), String.valueOf(totalDetails.getPointsInUse()),
                            String.valueOf(totalDetails.getPointsAcumulable()), gran_total.toString());
                }
                if (activity.getLeftFragment().orderRewardsFr != null) {
                    activity.getLeftFragment().orderRewardsFr.setRewardSubTotal(discountable_sub_total.toString());
                }
                activity.enableCheckoutButton();
                activity.getLeftFragment().mainLVAdapter.notifyDataSetChanged();
                activity.getLeftFragment().receiptListView.setSelection(activity.getLeftFragment().mainLVAdapter.selectedPosition);
            }
        }
    }

    private class MySpinnerAdapter extends ArrayAdapter<String> {
        public List<String> getLeftData() {
            return leftData;
        }

        List<String> leftData = null;

        public List<String[]> getRightData() {
            return rightData;
        }

        List<String[]> rightData = null;
        boolean isTax = false;
        private Activity context;

        MySpinnerAdapter(Activity activity, int resource, List<String> left, List<String[]> right,
                         boolean isTax) {
            super(activity, resource, left);
            this.context = activity;
            this.leftData = left;
            this.rightData = right;
            this.isTax = isTax;
        }

        int getDiscountIdPosition(String discountId) {
            for (int i = 0; i < rightData.size(); i++) {
                if (rightData.get(i)[4].equalsIgnoreCase(discountId)) {
                    return i;
                }
            }
            return -1;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            // we know that simple_spinner_item has android.R.id.text1 TextView:
            TextView text = view.findViewById(android.R.id.text1);
            text.setTextAppearance(activity, R.style.black_text_appearance);// choose your color
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    activity.getResources().getDimension(R.dimen.ordering_checkout_btn_txt_size));
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                row = inflater.inflate(R.layout.spinner_layout, parent, false);
            }

            TextView taxName = row.findViewById(R.id.taxName);
            TextView taxValue = row.findViewById(R.id.taxValue);
            ImageView checked = row.findViewById(R.id.checkMark);
            checked.setVisibility(View.INVISIBLE);
            taxName.setText(leftData.get(position));
            int type = getItemViewType(position);
            switch (type) {
                case 0: {
                    taxValue.setText("");
                    break;
                }
                case 1: {
                    setValues(taxValue, position);
                    checked.setVisibility(View.VISIBLE);
                    break;
                }
                case 2: {
                    setValues(taxValue, position);
                    break;
                }
            }
            return row;
        }

        void setValues(TextView taxValue, int position) {
            StringBuilder sb = new StringBuilder();
            if (isTax) {
                sb.append("%").append(rightData.get(position - 1)[2]);
                taxValue.setText(sb.toString());
            } else {
                if (rightData.get(position - 1)[1].equals("Fixed")) {
                    sb.append(Global.getCurrencyFormat(rightData.get(position - 1)[2]));
                    taxValue.setText(sb.toString());
                } else {
                    sb.append("%").append(rightData.get(position - 1)[2]);
                    taxValue.setText(sb.toString());
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return 0;
            } else if ((isTax && position == taxSelected) || (!isTax && position == discountSelected)) {
                return 1;
            }
            return 2;
        }
    }
    public List<Discount> getDiscountList() {
        return discountList;
    }
    public int getDiscountSelected() {
        return discountSelected;
    }
    private void showDiscountDlog() {
        try{
            final Dialog globalDlog = new Dialog(getActivity(), R.style.Theme_TransparentTest);
            globalDlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            globalDlog.setCancelable(true);
            globalDlog.setCanceledOnTouchOutside(true);
            globalDlog.setContentView(R.layout.dlog_field_single_layout);

            final EditText viewField = globalDlog.findViewById(R.id.dlogFieldSingle);
            viewField.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
            try{
                if (myPref.getOpenDiscount() != null) {
                    viewField.setText(myPref.getOpenDiscount());
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            TextView viewTitle = globalDlog.findViewById(R.id.dlogTitle);
            TextView viewMsg = globalDlog.findViewById(R.id.dlogMessage);
            viewTitle.setText(R.string.dlog_title_confirm);
            viewTitle.setText(R.string.enter_open_discount);
            viewMsg.setVisibility(View.GONE);
            Button btnCancel = globalDlog.findViewById(R.id.btnCancelDlogSingle);
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    globalDlog.dismiss();
                }
            });
            Button btnOk = globalDlog.findViewById(R.id.btnDlogSingle);
            btnOk.setText(R.string.button_ok);
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    globalDlog.dismiss();
                    String value = viewField.getText().toString().trim();
                    DecimalFormat frmt = new DecimalFormat("0.00");
                    try{
                        updateDiscount(value,discountSpinner.getSelectedItemPosition());
                    }catch (Exception x){
                        Toast.makeText(activity,x.getMessage(), Toast.LENGTH_LONG).show();
                        x.printStackTrace();
                    }
                }
            });
            globalDlog.show();
        }catch (Exception x){
            x.printStackTrace();
        }
    }
    private void askForManagerPassDlg()        //type 0 = Change Password, type 1 = Configure Home Menu
    {
        final Dialog globalDlog = new Dialog(activity, R.style.Theme_TransparentTest);
        globalDlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        globalDlog.setCancelable(true);
        globalDlog.setCanceledOnTouchOutside(true);
        globalDlog.setContentView(R.layout.dlog_field_single_layout);

        final EditText viewField = globalDlog.findViewById(R.id.dlogFieldSingle);
        viewField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        TextView viewTitle = globalDlog.findViewById(R.id.dlogTitle);
        TextView viewMsg = globalDlog.findViewById(R.id.dlogMessage);
        viewTitle.setText(R.string.dlog_title_confirm);
//        if (!validPassword)
//            viewMsg.setText(R.string.invalid_password);
//        else
            viewMsg.setText(R.string.dlog_title_enter_manager_password);
        Button btnCancel = globalDlog.findViewById(R.id.btnCancelDlogSingle);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                globalDlog.dismiss();
            }
        });

        Button btnOk = globalDlog.findViewById(R.id.btnDlogSingle);
        btnOk.setText(R.string.button_ok);
        btnOk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                globalDlog.dismiss();
                String value = viewField.getText().toString();
                if (myPref.loginManager(value)) // validate manager password
                {
                    validPassword = true;
                    showDiscountDlog();
                } else {
                    validPassword = false;
//                    askForManagerPassDlg();
                }
            }
        });
        globalDlog.show();
    }
    private void updateDiscount(String discountPercent,int pos){
        try{
            List<Discount> newDiscountList = new ArrayList<Discount>();
            List<String> discList = ((MySpinnerAdapter) discountSpinner.getAdapter()).getLeftData();
            List<String> leftDiscList = new ArrayList<String>();
            List<String[]> discountArr = new ArrayList<>();
            int count = 0;
            leftDiscList.add(discList.get(0));
            for (Discount disc : discountList) {
                count++;
                String[] arr = new String[5];
                arr[0] = disc.getProductName();
                arr[1] = disc.getProductDiscountType();
                if(count == pos){
                    disc.setProductPrice(discountPercent);
                    arr[0] = disc.getDiscountName();
                    arr[2] = discountPercent;
                }else{
                    arr[2] = disc.getProductPrice();
                }
                arr[3] = disc.getTaxCodeIsTaxable();
                arr[4] = disc.getProductId();
                leftDiscList.add(arr[0]);
                discountArr.add(arr);
                newDiscountList.add(new Discount(arr));
            }
            discountList = newDiscountList;
            MySpinnerAdapter discountAdapter = new MySpinnerAdapter(activity, android.R.layout.simple_spinner_item, leftDiscList, discountArr,
                    false);
            discountSpinner.setAdapter(discountAdapter);
            discountSpinner.setOnItemSelectedListener(setSpinnerListener(true));
            discountSpinner.setSelection(0);// This selects the first item in the discount spinner
            discountSpinner.setSelection(pos);
        }catch (Exception x){
            x.printStackTrace();
        }
    }
}