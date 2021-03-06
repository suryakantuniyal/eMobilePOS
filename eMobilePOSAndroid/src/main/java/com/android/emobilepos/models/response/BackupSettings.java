package com.android.emobilepos.models.response;

import android.content.Context;

import com.android.emobilepos.R;

import com.android.emobilepos.models.response.restoresettings.HomeMenuConfig;
import com.android.emobilepos.models.response.restoresettings.kioskSettings;
import com.android.emobilepos.models.response.restoresettings.OtherSettings;
import com.android.emobilepos.models.response.restoresettings.PrintPrefs;
import com.android.support.MyPreferences;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BackupSettings {

    private MyPreferences myPreferences;
    private Gson gson = new Gson();
    private Context context;

    public BackupSettings(MyPreferences myPreferences,Context context) {
        this.myPreferences = myPreferences;
        this.context = context;
    }

    public void restoreMySettings(BuildSettings[] mSettings) {
        for (BuildSettings build : mSettings) {
            ConfigSettings(build, true, null, null);
        }
    }

    public String backupMySettings(String empID, String regID) {
        BuildSettings build = new BuildSettings();
        BuildSettings mBuild = ConfigSettings(build, false, empID, regID);
        String json = gson.toJson(mBuild);
        return json;
    }

    private BuildSettings ConfigSettings(BuildSettings build, boolean isRestore, String empID, String regID) {
        if (isRestore) {
            myPreferences.setPreferences("pref_automatic_sync", build.getGeneralSettings().isAutosync());
            myPreferences.setPreferences("pref_holds_polling_service", build.getGeneralSettings().isHoldsPollingService());
            myPreferences.setPreferencesValue("pref_transaction_num_prefix", build.getGeneralSettings().getTransNumPreffix());
            myPreferences.setPreferences("pref_fast_scanning_mode", build.getGeneralSettings().isFastScanning());
            myPreferences.setPreferences("pref_signature_required_mode", build.getGeneralSettings().isSignatureRequired());
            myPreferences.setPreferences("pref_qr_code_reading", build.getGeneralSettings().isQRReadFromCam());
            myPreferences.setPreferences("pref_enable_multi_category", build.getGeneralSettings().isMultipleCatPerProd());
            myPreferences.setPreferences("pref_show_only_group_taxes", build.getGeneralSettings().isGroupTaxes());
            myPreferences.setPreferences("pref_retail_taxes", build.getGeneralSettings().isRetailTaxes());
            myPreferences.setPreferences("pref_mix_match", build.getGeneralSettings().isMixNMatch());
            myPreferences.setPreferences("pref_show_confirmation_screen", build.getGeneralSettings().isConfirmationScreen());
            myPreferences.setPreferences("pref_ask_order_comments", build.getGeneralSettings().isAskForComments());
            myPreferences.setPreferences("pref_skip_email_phone", build.getGeneralSettings().isSkipContactInfoPrompt());
            myPreferences.setPreferences("pref_skip_want_add_more_products", build.getGeneralSettings().isSkipAddMoreProducts());
            myPreferences.setPreferences("pref_require_shift_transactions", build.getGeneralSettings().isRequireShift());
            myPreferences.setPreferences("pref_scope_bar_in_restaurant_mode", build.getGeneralSettings().isRestaurantModeShowScopeBar());
            myPreferences.setPreferences("pref_use_clerks", build.getGeneralSettings().isUseClerks());
            myPreferences.setPreferences("pref_use_clerks_autologout", build.getGeneralSettings().isClerkAutoLogOut());

            //CUSTOMER SETTINGS
            myPreferences.setPreferences("pref_require_customer", build.getCustomerSettings().isRequired());
            myPreferences.setPreferences("pref_clear_customer", build.getCustomerSettings().isClearAfterTrans());
            myPreferences.setPreferencesValue("pref_default_customer_display_name", build.getCustomerSettings().getSelectTag());
            myPreferences.setPreferences("pref_direct_customer_selection", build.getCustomerSettings().isDirectSelection());
            myPreferences.setPreferences("pref_display_customer_account_number", build.getCustomerSettings().isDisplayAccountNumber());
            myPreferences.setPreferences("pref_allow_customer_creation", build.getCustomerSettings().isAllowCreation());

            //RESTAURANT SETTINGS
            myPreferences.setPreferences("pref_restaurant_mode", build.getRestaurantSettings().isMode());
            myPreferences.setPreferences("pref_enable_togo_eatin", build.getRestaurantSettings().isOrderOptions());
            myPreferences.setPreferences("pref_enable_table_selection", build.getRestaurantSettings().isTableSelection());
            myPreferences.setPreferences("pref_ask_seats", build.getRestaurantSettings().isNumberOfSeats());

            //GIFT CARD SETTINGS
            myPreferences.setPreferences("pref_display_also_redeem", build.getGiftCardSettings().isShowAlsoRedeem());
            myPreferences.setPreferences("pref_display_redeem_all", build.getGiftCardSettings().isShowRedeemAll());
            myPreferences.setPreferencesValue("pref_units_name", build.getGiftCardSettings().getUnitName());
            myPreferences.setPreferences("pref_use_loyal_patron", build.getGiftCardSettings().isTupyxGift());
            myPreferences.setPreferences("pref_giftcard_auto_balance_request", build.getGiftCardSettings().isAutoBalanceRequest());
            myPreferences.setPreferences("pref_use_stadis_iv", build.getGiftCardSettings().isUseStadisV4());

            //PAYMENT METHOD SETTINGS
            myPreferences.setPreferences("pref_pay_with_tupyx", build.getPaymentMethodSettings().isPayWithTupyx());
            myPreferences.setPreferences("pref_mw_with_genius", build.getPaymentMethodSettings().isCayanGenius());
            myPreferences.setPreferences("pref_pay_with_card_on_file", build.getPaymentMethodSettings().isPayWithCardOnFile());
            myPreferences.setPreferences("pref_use_pax", build.getPaymentMethodSettings().isPAXSecurePay());
            myPreferences.setPreferences("pref_use_sound_payments", build.getPaymentMethodSettings().isSPSecurePay());
            myPreferences.setPreferencesValue("pref_config_genius_peripheral", build.getPaymentMethodSettings().getGeniusIP());

            //PAYMENT PROCESSING SETTINGS
            myPreferences.setPreferences("pref_allow_manual_credit_card", build.getPaymentProcessingSettings().isAllowManualCreditCard());
            myPreferences.setPreferences("pref_process_check_online", build.getPaymentProcessingSettings().isProcessCheckOnline());
            myPreferences.setPreferences("pref_show_tips_for_cash", build.getPaymentProcessingSettings().isShowTipsForCash());
            myPreferences.setPreferencesValue("pref_audio_card_reader", build.getPaymentProcessingSettings().getAudioCardReader());
            myPreferences.setPreferencesValue("pref_default_payment_method", build.getPaymentProcessingSettings().getDefaultPaymentMethod());
            myPreferences.setPreferences("pref_return_require_refund", build.getPaymentProcessingSettings().isReturnRequireRefund());
            myPreferences.setPreferences("pref_convert_to_reward", build.getPaymentProcessingSettings().isConvertToReward());
            myPreferences.setPreferences("pref_invoice_require_payment", build.getPaymentProcessingSettings().isInvoiceRequirePayment());
            myPreferences.setPreferences("pref_invoice_require_full_payment", build.getPaymentProcessingSettings().isInvoiceRequirePaymentFull());
            myPreferences.setPreferences("pref_prefill_total_amount", build.getPaymentProcessingSettings().isPreFillTotalAmount());
            myPreferences.setPreferences("pref_use_store_and_forward", build.getPaymentProcessingSettings().isUseStoreForward());
            myPreferences.setPreferences("pref_cash_show_change", build.getPaymentProcessingSettings().isShowCashChangeAmount());

            //PRINTING SETTINGS
            myPreferences.setPreferences("pref_enable_printing", build.getPrintingSettings().isEnabled());
            myPreferences.setPreferences("pref_automatic_printing", build.getPrintingSettings().isAutomaticPrinting());
            myPreferences.setPreferences("pref_enable_multiple_prints", build.getPrintingSettings().isMupltiplePrints());
            myPreferences.setPreferences("pref_use_permitreceipt_printing", build.getPrintingSettings().isPermitReceipt());
            myPreferences.setPreferencesValue("pref_printer_width", build.getPrintingSettings().getPrinterWidth());
            myPreferences.setPreferences("pref_split_stationprint_by_categories", build.getPrintingSettings().isSplitStationByCategories());
            myPreferences.setPreferences("pref_wholesale_printout", build.getPrintingSettings().isWholesalePrintOut());
            myPreferences.setPreferences("pref_handwritten_signature", build.getPrintingSettings().isHandwrittenSignature());
            myPreferences.setPreferences("pref_prompt_customer_copy", build.getPrintingSettings().isPromptReceiptCC());
            myPreferences.setPreferences("pref_print_receipt_transaction_payment", build.getPrintingSettings().isPrintTransPayments());
            myPreferences.setPreferences("pref_print_taxes_breakdown", build.getPrintingSettings().isPrintTaxesBreakdown());

            myPreferences.setPreferencesValue("pref_star_info", build.getPrintingSettings().getStarInfo());
            myPreferences.setPreferencesValue("pref_snbc_setup", build.getPrintingSettings().getSNBCSetup());
//            PrintingSetting mConfig = build.getPrintingSettings();
//            mConfig.getBixolonSetup();
//            myPreferences.setPreferencesValue("pref_bixolon_setup",build.getPrintingSettings().getBixolonSetup());

            PrintPrefs printPref = build.getPrintingSettings().getPrintPrefs();
            Set<String> values = new HashSet<>();
            values.add(String.valueOf(printPref.isHeader()));
            values.add(String.valueOf(printPref.isShipToInfo()));
            values.add(String.valueOf(printPref.isTerms()));
            values.add(String.valueOf(printPref.isCustomerAccNumber()));
            values.add(String.valueOf(printPref.isOrderComments()));
            values.add(String.valueOf(printPref.isAddons()));
            values.add(String.valueOf(printPref.isProductTaxDetails()));
            values.add(String.valueOf(printPref.isProductDiscountDetails()));
            values.add(String.valueOf(printPref.isProductDescriptions()));
            values.add(String.valueOf(printPref.isProductComments()));
            values.add(String.valueOf(printPref.isSaleAttributes()));
            values.add(String.valueOf(printPref.isPaymentComments()));
            values.add(String.valueOf(printPref.isIVULotoQRCode()));
            values.add(String.valueOf(printPref.isFooter()));
            values.add(String.valueOf(printPref.isTermsAndConditions()));
            values.add(String.valueOf(printPref.isEMSWebsiteFooter()));
            myPreferences.setPrintingPreferences(values);
//            myPreferences.setPreferences("pref_set_printing_preferences", build.getPrintingSettings().getPrintPrefs());
            myPreferences.setPreferences("pref_print_raster_mode", build.getPrintingSettings().isPrintRasterMode());

            //PRODUCT SETTINGS
            myPreferences.setPreferences("pref_allow_decimal_quantities", build.getProductSettings().isAllowDecimalQuantities());
            myPreferences.setPreferences("pref_remove_leading_zeros", build.getProductSettings().isRemoveLeadZerosUPCSKU());
            myPreferences.setPreferences("pref_group_receipt_by_sku", build.getProductSettings().isGroupReceiptBySKU());
            myPreferences.setPreferences("pref_require_password_to_remove_void", build.getProductSettings().isRequirePasswordRemoveVoid());
            myPreferences.setPreferences("pref_show_removed_void_items_in_printout", build.getProductSettings().isShowRemovedItemsOnPrintOut());
            myPreferences.setPreferencesValue("pref_default_category", build.getProductSettings().getDefaultCategory());
            myPreferences.setPreferencesValue("pref_attribute_to_display", build.getProductSettings().getAttributeToDisplay());
            myPreferences.setPreferences("pref_group_in_catalog_by_name", build.getProductSettings().isGroupInCatalogByName());
            myPreferences.setPreferences("pref_filter_products_by_customer", build.getProductSettings().isFilterByCustomers());
            myPreferences.setPreferences("pref_limit_products_on_hand", build.getProductSettings().isLimitProductsOnHand());

            //ACCOUNT SETTINGS
            myPreferences.setPreferences("pref_expire_session", build.getSessionSettings().isExpireUserLoginSession());
            myPreferences.setPreferencesValue("pref_expire_usersession_time", build.getSessionSettings().getUserSessionExpirationTime());

            //KIOSK CONFIGURATION
            kioskSettings mKiosk = build.getKioskSettings();
            myPreferences.cdtLine1(false, mKiosk.getCustomerDisplayTerminal().getDisplayLine1());
            myPreferences.cdtLine2(false, mKiosk.getCustomerDisplayTerminal().getDisplayLine2());

            //SHIPPING CALCULATION
            myPreferences.setPreferences("config_use_nexternal", build.getShippingCalculation().isUseNexternal());

            //TRANSACTION SETTINGS
            myPreferences.setPreferences("pref_require_manager_pass_to_void_trans", build.getTransactionSettings().isRequireManagerPWToVoid());
            myPreferences.setPreferencesValue("pref_default_country", build.getTransactionSettings().getDefaultCountry());

            //OTHER SETTINGS
            OtherSettings mOther = build.getOtherSettings();
            ArrayList<String> homeMenuArrValues = new ArrayList<>();
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isSalesReceipt()) ? "0" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isOrder()) ? "1" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isReturn()) ? "2" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isInvoice()) ? "3" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isEstimate()) ? "4" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isPayment()) ? "5" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isGiftCard()) ? "6" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isLoyaltyCard()) ? "7" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isRewardCard()) ? "8" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isRefund()) ? "9" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isRoute()) ? "10" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isHolds()) ? "11" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isConsignment()) ? "12" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isInventoryTransfer()) ? "13" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isTipAdjustment()) ? "14" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isShifts()) ? "15" : "");
            homeMenuArrValues.add((mOther.getHomeMenuConfig().isNoSale()) ? "16" : "");
            myPreferences.setMainMenuSettings(homeMenuArrValues);

            myPreferences.setPreferencesValue("pref_default_transaction", mOther.getDefaultTransaction());
            myPreferences.setPreferences("pref_block_price_level_change", mOther.isBlockPriceLevelChange());
            myPreferences.setPreferences("pref_require_address", mOther.isRequireAddress());
            myPreferences.setPreferences("pref_require_po", mOther.isRequirePO());
            myPreferences.setPreferences("pref_skip_manager_price_override", mOther.isSkipManagerPriceOverride());
            myPreferences.setPreferences("pref_require_password_to_clockout", mOther.isRequirePWToClockOut());
            myPreferences.setPreferences("pref_maps_inside_app", mOther.isMapsInsideApp());
            myPreferences.setPreferences("pref_enable_location_inventory", mOther.isUseLocationInventory());

            //SYNC PLUS SERVICES
            myPreferences.setPreferences("pref_use_syncplus_services", build.getSyncPlusServices().isUseSyncPlusServices());
            myPreferences.setPreferences("pref_syncplus_mode", !build.getSyncPlusServices().getConnectionMode().equalsIgnoreCase("automatic"));
            myPreferences.setPreferencesValue("pref_syncplus_ip", build.getSyncPlusServices().getIPAddress());
            myPreferences.setPreferencesValue("pref_syncplus_port", build.getSyncPlusServices().getPortNumber());

            return null;
        } else {
            build.setRegid(regID);
            build.setEmpid(Integer.valueOf(empID));
            build.getGeneralSettings().setAutosync(myPreferences.getPreferences("pref_automatic_sync"));
            build.getGeneralSettings().setHoldsPollingService(myPreferences.getPreferences("pref_holds_polling_service"));
            build.getGeneralSettings().setTransNumPreffix(myPreferences.getPreferencesValue("pref_transaction_num_prefix"));
            build.getGeneralSettings().setFastScanning(myPreferences.getPreferences("pref_fast_scanning_mode"));
            build.getGeneralSettings().setSignatureRequired(myPreferences.getPreferences("pref_signature_required_mode"));
            build.getGeneralSettings().setQRReadFromCam(myPreferences.getPreferences("pref_qr_code_reading"));
            build.getGeneralSettings().setMultipleCatPerProd(myPreferences.getPreferences("pref_enable_multi_category"));
            build.getGeneralSettings().setGroupTaxes(myPreferences.getPreferences("pref_show_only_group_taxes"));
            build.getGeneralSettings().setRetailTaxes(myPreferences.getPreferences("pref_retail_taxes"));
            build.getGeneralSettings().setMixNMatch(myPreferences.getPreferences("pref_mix_match"));
            build.getGeneralSettings().setConfirmationScreen(myPreferences.getPreferences("pref_show_confirmation_screen"));
            build.getGeneralSettings().setAskForComments(myPreferences.getPreferences("pref_ask_order_comments"));
            build.getGeneralSettings().setSkipContactInfoPrompt(myPreferences.getPreferences("pref_skip_email_phone"));
            build.getGeneralSettings().setSkipAddMoreProducts(myPreferences.getPreferences("pref_skip_want_add_more_products"));
            build.getGeneralSettings().setRequireShift(myPreferences.getPreferences("pref_require_shift_transactions"));
            build.getGeneralSettings().setRestaurantModeShowScopeBar(myPreferences.getPreferences("pref_scope_bar_in_restaurant_mode"));
            build.getGeneralSettings().setUseClerks(myPreferences.getPreferences("pref_use_clerks"));
            build.getGeneralSettings().setClerkAutoLogOut(myPreferences.getPreferences("pref_use_clerks_autologout"));

            //CUSTOMER SETTINGS
            build.getCustomerSettings().setRequired(myPreferences.getPreferences("pref_require_customer"));
            build.getCustomerSettings().setClearAfterTrans(myPreferences.getPreferences("pref_clear_customer"));
            build.getCustomerSettings().setSelectTag(myPreferences.getPreferencesValue("pref_default_customer_display_name").toLowerCase());
            build.getCustomerSettings().setDirectSelection(myPreferences.getPreferences("pref_direct_customer_selection"));
            build.getCustomerSettings().setDisplayAccountNumber(myPreferences.getPreferences("pref_display_customer_account_number"));
            build.getCustomerSettings().setAllowCreation(myPreferences.getPreferences("pref_allow_customer_creation"));

            //RESTAURANT SETTINGS
            build.getRestaurantSettings().setMode(myPreferences.getPreferences("pref_restaurant_mode"));
            build.getRestaurantSettings().setOrderOptions(myPreferences.getPreferences("pref_enable_togo_eatin"));
            build.getRestaurantSettings().setTableSelection(myPreferences.getPreferences("pref_enable_table_selection"));
            build.getRestaurantSettings().setNumberOfSeats(myPreferences.getPreferences("pref_ask_seats"));

            //GIFT CARD SETTINGS
            build.getGiftCardSettings().setShowAlsoRedeem(myPreferences.getPreferences("pref_display_also_redeem"));
            build.getGiftCardSettings().setShowRedeemAll(myPreferences.getPreferences("pref_display_redeem_all"));
            build.getGiftCardSettings().setUnitName(myPreferences.getDefaultUnitsName());//.getPreferencesValue("pref_units_name"));
            build.getGiftCardSettings().setTupyxGift(myPreferences.getPreferences("pref_use_loyal_patron"));
            build.getGiftCardSettings().setAutoBalanceRequest(myPreferences.getPreferences("pref_giftcard_auto_balance_request"));
            build.getGiftCardSettings().setUseStadisV4(myPreferences.getPreferences("pref_use_stadis_iv"));

            //PAYMENT METHOD SETTINGS
            build.getPaymentMethodSettings().setPayWithTupyx(myPreferences.getPreferences("pref_pay_with_tupyx"));
            build.getPaymentMethodSettings().setCayanGenius(myPreferences.getPreferences("pref_mw_with_genius"));
            build.getPaymentMethodSettings().setPayWithCardOnFile(myPreferences.getPreferences("pref_pay_with_card_on_file"));
            build.getPaymentMethodSettings().setPAXSecurePay(myPreferences.getPreferences("pref_use_pax"));
            build.getPaymentMethodSettings().setSPSecurePay(myPreferences.getPreferences("pref_use_sound_payments"));
            build.getPaymentMethodSettings().setGeniusIP(myPreferences.getGeniusIP());//myPreferences.getPreferencesValue("pref_config_genius_peripheral"));

            //PAYMENT PROCESSING SETTINGS
            String audioReader = myPreferences.getPreferencesValue("pref_audio_card_reader");
            switch (audioReader) {
                case "-1":
                    audioReader = "";
                    break;
                case "0":
                    audioReader = "unimag";
                    break;
                case "1":
                    audioReader = "magtek";
                    break;
                case "2":
                    audioReader = "rover";
                    break;
                case "3":
                    audioReader = "walker";
                    break;
            }
            build.getPaymentProcessingSettings().setAllowManualCreditCard(myPreferences.getPreferences("pref_allow_manual_credit_card"));
            build.getPaymentProcessingSettings().setProcessCheckOnline(myPreferences.getPreferences("pref_process_check_online"));
            build.getPaymentProcessingSettings().setShowTipsForCash(myPreferences.getPreferences("pref_show_tips_for_cash"));
            build.getPaymentProcessingSettings().setAudioCardReader(audioReader);
            build.getPaymentProcessingSettings().setDefaultPaymentMethod(myPreferences.getPreferencesValue("pref_default_payment_method"));
            build.getPaymentProcessingSettings().setReturnRequireRefund(myPreferences.getPreferences("pref_return_require_refund"));
            build.getPaymentProcessingSettings().setConvertToReward(myPreferences.getPreferences("pref_convert_to_reward"));
            build.getPaymentProcessingSettings().setInvoiceRequirePayment(myPreferences.getPreferences("pref_invoice_require_payment"));
            build.getPaymentProcessingSettings().setInvoiceRequirePaymentFull(myPreferences.getPreferences("pref_invoice_require_full_payment"));
            build.getPaymentProcessingSettings().setPreFillTotalAmount(myPreferences.getPreferences("pref_prefill_total_amount"));
            build.getPaymentProcessingSettings().setUseStoreForward(myPreferences.getPreferences("pref_use_store_and_forward"));
            build.getPaymentProcessingSettings().setShowCashChangeAmount(myPreferences.getPreferences("pref_cash_show_change"));

            //PRINTING SETTINGS
            build.getPrintingSettings().setEnabled(myPreferences.getPreferences("pref_enable_printing"));
            build.getPrintingSettings().setAutomaticPrinting(myPreferences.getPreferences("pref_automatic_printing"));
            build.getPrintingSettings().setMupltiplePrints(myPreferences.getPreferences("pref_enable_multiple_prints"));
            build.getPrintingSettings().setPermitReceipt(myPreferences.getPreferences("pref_use_permitreceipt_printing"));
            build.getPrintingSettings().setPrinterWidth(myPreferences.getPreferencesValue("pref_printer_width").toLowerCase());
            build.getPrintingSettings().setSplitStationByCategories(myPreferences.getPreferences("pref_split_stationprint_by_categories"));
            build.getPrintingSettings().setWholesalePrintOut(myPreferences.getPreferences("pref_wholesale_printout"));
            build.getPrintingSettings().setHandwrittenSignature(myPreferences.getPreferences("pref_handwritten_signature"));
            build.getPrintingSettings().setPromptReceiptCC(myPreferences.getPreferences("pref_prompt_customer_copy"));
            build.getPrintingSettings().setPrintTransPayments(myPreferences.getPreferences("pref_print_receipt_transaction_payment"));
            build.getPrintingSettings().setPrintTaxesBreakdown(myPreferences.getPreferences("pref_print_taxes_breakdown"));

            build.getPrintingSettings().setStarInfo(myPreferences.getStarIPAddress());//.getPreferencesValue("pref_star_info"));
            build.getPrintingSettings().setSNBCSetup(myPreferences.getPreferencesValue("pref_snbc_setup"));
//            BixolonSetupSetting mConfig = build.getPrintingSettings().getBixolonSetup();
//            myPreferences.getPreferencesValue("pref_bixolon_setup");

            List<String> list = myPreferences.getPrintingPreferences();
            HashMap<String, Boolean> mValues = new HashMap<>();
            String[] prefValues = context.getResources().getStringArray(R.array.printPrefValues);
            List<String> arr = Arrays.asList(prefValues);
            PrintPrefs PrintPrefs = build.getPrintingSettings().getPrintPrefs();
            for (int i = 0; i < prefValues.length; i++) {
                if(list.contains(arr.get(i)))
                    mValues.put(arr.get(i),true);
//                    mValues.add(i,true);
                else
                    mValues.put(arr.get(i),false);
//                    mValues.add(i,false);
            }
            PrintPrefs.setHeader(mValues.get("print_header"));
            PrintPrefs.setShipToInfo(mValues.get("print_shiptoinfo"));
            PrintPrefs.setTerms(mValues.get("print_terms"));
            PrintPrefs.setCustomerAccNumber(mValues.get("print_customer_id"));
            PrintPrefs.setOrderComments(mValues.get("print_order_comments"));
            PrintPrefs.setAddons(mValues.get("print_addons"));
            PrintPrefs.setProductTaxDetails(mValues.get("print_tax_details"));
            PrintPrefs.setProductDiscountDetails(mValues.get("print_discount_details"));
            PrintPrefs.setProductDescriptions(mValues.get("print_descriptions"));
            PrintPrefs.setProductComments(mValues.get("print_prod_comments"));
            PrintPrefs.setSaleAttributes(mValues.get("print_sale_attributes"));
            PrintPrefs.setPaymentComments(mValues.get("print_payment_comments"));
            PrintPrefs.setIVULotoQRCode(mValues.get("print_ivu_loto_qr"));
            PrintPrefs.setFooter(mValues.get("print_footer"));
            PrintPrefs.setTermsAndConditions(mValues.get("print_terms_conditions"));
            PrintPrefs.setEMSWebsiteFooter(mValues.get("print_emobilepos_website"));
            build.getPrintingSettings().setPrintPrefs(PrintPrefs);
            build.getPrintingSettings().setPrintRasterMode(myPreferences.getPreferences("pref_print_raster_mode"));

            //PRODUCT SETTINGS
            build.getProductSettings().setAllowDecimalQuantities(myPreferences.getPreferences("pref_allow_decimal_quantities"));
            build.getProductSettings().setRemoveLeadZerosUPCSKU(myPreferences.getPreferences("pref_remove_leading_zeros"));
            build.getProductSettings().setGroupReceiptBySKU(myPreferences.getPreferences("pref_group_receipt_by_sku"));
            build.getProductSettings().setRequirePasswordRemoveVoid(myPreferences.getPreferences("pref_require_password_to_remove_void"));
            build.getProductSettings().setShowRemovedItemsOnPrintOut(myPreferences.getPreferences("pref_show_removed_void_items_in_printout"));
            build.getProductSettings().setDefaultCategory(myPreferences.getPreferencesValue("pref_default_category"));
            build.getProductSettings().setAttributeToDisplay(myPreferences.getPreferencesValue("pref_attribute_to_display").replace("_", "").toLowerCase());
            build.getProductSettings().setGroupInCatalogByName(myPreferences.getPreferences("pref_group_in_catalog_by_name"));
            build.getProductSettings().setFilterByCustomers(myPreferences.getPreferences("pref_filter_products_by_customer"));
            build.getProductSettings().setLimitProductsOnHand(myPreferences.getPreferences("pref_limit_products_on_hand"));

            //ACCOUNT SETTINGS
            build.getSessionSettings().setExpireUserLoginSession(myPreferences.getPreferences("pref_expire_session"));
            build.getSessionSettings().setUserSessionExpirationTime(myPreferences.getPreferencesValue("pref_expire_usersession_time"));

            //KIOSK CONFIGURATION
            kioskSettings mKiosk = build.getKioskSettings();
            mKiosk.getCustomerDisplayTerminal().setDisplayLine1(myPreferences.cdtLine1(true, null));
            mKiosk.getCustomerDisplayTerminal().setDisplayLine2(myPreferences.cdtLine2(true, null));

            //SHIPPING CALCULATION
            build.getShippingCalculation().setUseNexternal(myPreferences.getPreferences("config_use_nexternal"));

            //TRANSACTION SETTINGS
            build.getTransactionSettings().setRequireManagerPWToVoid(myPreferences.getPreferences("pref_require_manager_pass_to_void_trans"));
            build.getTransactionSettings().setDefaultCountry(myPreferences.getDefaultCountryName());

            //OTHER SETTINGS
            OtherSettings mOther = build.getOtherSettings();
            HomeMenuConfig home = mOther.getHomeMenuConfig();
            boolean[] values = myPreferences.getMainMenuPreference();
            home.setSalesReceipt(values[0]);
            home.setOrder(values[1]);
            home.setReturn(values[2]);
            home.setInvoice(values[3]);
            home.setEstimate(values[4]);
            home.setPayment(values[5]);
            home.setGiftCard(values[6]);
            home.setLoyaltyCard(values[7]);
            home.setRewardCard(values[8]);
            home.setRefund(values[9]);
            home.setRoute(values[10]);
            home.setHolds(values[11]);
            home.setConsignment(values[12]);
            home.setInventoryTransfer(values[13]);
            home.setTipAdjustment(values[14]);
            home.setShifts(values[15]);
            home.setNoSale(values[16]);
            mOther.setHomeMenuConfig(home);

            String transaction = myPreferences.getPreferencesValue("pref_default_transaction");
            if(transaction == null || transaction.trim().equals("")){
                transaction = "-1";
            }
            mOther.setDefaultTransaction(transaction);
            mOther.setBlockPriceLevelChange(myPreferences.getPreferences("pref_block_price_level_change"));
            mOther.setRequireAddress(myPreferences.getPreferences("pref_require_address"));
            mOther.setRequirePO(myPreferences.getPreferences("pref_require_po"));
            mOther.setSkipManagerPriceOverride(myPreferences.getPreferences("pref_skip_manager_price_override"));
            mOther.setRequirePWToClockOut(myPreferences.getPreferences("pref_require_password_to_clockout"));
            mOther.setMapsInsideApp(myPreferences.getPreferences("pref_maps_inside_app"));
            mOther.setUseLocationInventory(myPreferences.getPreferences("pref_enable_location_inventory"));

            //SYNC PLUS SERVICES
            build.getSyncPlusServices().setUseSyncPlusServices(myPreferences.getPreferences("pref_use_syncplus_services"));
            build.getSyncPlusServices().setConnectionMode((myPreferences.getPreferences("pref_syncplus_mode")) ? "manual" : "automatic");
            build.getSyncPlusServices().setIPAddress(myPreferences.getPreferencesValue("pref_syncplus_ip"));
            build.getSyncPlusServices().setPortNumber(myPreferences.getPreferencesValue("pref_syncplus_port"));

            return build;
        }
    }
}
