package com.honeywell.decodemanager.barcode;

public final class CommonDefine
{
  public static final class ErrorCode
  {
    public static final int RESULT_INITIALIZE = -1;
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_ERR_BADREGION = 1;
    public static final int RESULT_ERR_DRIVER = 2;
    public static final int RESULT_ERR_ENGINEBUSY = 3;
    public static final int RESULT_ERR_MEMORY = 4;
    public static final int RESULT_ERR_NODECODE = 5;
    public static final int RESULT_ERR_NOIMAGE = 6;
    public static final int RESULT_ERR_NORESPONSE = 7;
    public static final int RESULT_ERR_NOTCONNECTED = 8;
    public static final int RESULT_ERR_PARAMETER = 9;
    public static final int RESULT_ERR_UNSUPPORTED = 10;
    public static final int RESULT_ERR_NOTRIGGER = 11;
    public static final int RESULT_ERR_BADSMARTIMAGE = 12;
    public static final int RESULT_ERR_SMARTIMAGETOOLARGE = 13;
    public static final int RESULT_ERR_TOO_MUCH_INTERPOLATION = 14;
    public static final int RESULT_ERR_WRONGRESULTSTRUCT = 15;
    public static final int RESULT_ERR_THREAD = 16;
    public static final int RESULT_ERR_CANCEL = 17;
    public static final int RESULT_ERR_EXCEPTION = 18;
    public static final int RESULT_ERR_UNSUPPORTED_IQ_BARCODE = 19;
    public static final int RESULT_ERR_LOAD_EXMFILE = 20;
    public static final int RESULT_ERR_EXMFILE_INVALID = 21;
    public static final int RESULT_ERR_MISSING_EXMSECTION = 22;
    public static final int RESULT_ERR_PROCESSING_EXMSECTION = 23;
    public static final int RESULT_ALLOW_IMAGE_PROCESS = 24;
    public static final int RESULT_ALLOW_PREVIEW_PROCESS = 25;
    public static final int RESULT_STOP_PREVIEW_PROCESS = 26;
  }

  public static final class ExposureReturnCode
  {
    public static final int Initialize = -1;
    public static final int Success = 0;
    public static final int ErrorMemoryAllocation = 1;
    public static final int ErrorMemoryFree = 2;
    public static final int ErrorFileCreation = 3;
    public static final int ErrorFileWrite = 4;
    public static final int ErrorInvalidDIB = 5;
    public static final int ErrorUnknown = 6;
    public static final int ErrorInvalidParm = 7;
    public static final int ErrorAlreadyInitialized = 8;
    public static final int ErrorNotInitialized = 9;
    public static final int ErrorDuplicateProfile = 10;
    public static final int ErrorReadOnlyProfile = 11;
    public static final int ErrorProfileNotFound = 12;
    public static final int ErrorInvalidProfile = 13;
    public static final int ErrorAlreadyCapturing = 14;
    public static final int ErrorNotCapturing = 15;
    public static final int ErrorFileRead = 16;
    public static final int ErrorConfigFileRead = 17;
  }

  public static final class ExposureMode
  {
    public static final int FIXED = 0;
    public static final int ONCHIP = 1;
    public static final int HHP = 2;
    public static final int AUTO_PRESENTATION = 3;
    public static final int CONTEXT_SENSITIVE = 4;
    public static final int OPENLOOP_GAIN = 5;
    public static final int CELLPHONE = 6;
    public static final int AUTO_DUAL_TRACK = 7;
    public static final int CONTEXT_DUAL_TRACK = 8;
    public static final int TEST_PATTERN = 9;
  }

  public static final class ScanMode
  {
    public static final int BATCH = 0;
    public static final int STREAM = 1;
    public static final int CONCURRENT_IQ = 2;
    public static final int CONCURRENT_FAST = 3;
    public static final int CONCURRENT_FAST_IQ = 4;
    public static final int CONFIG_NUMBER = 5;
  }

  public static final class IQImageFormat
  {
    public static final int RAW_BINARY = 0;
    public static final int RAW_GRAY = 1;
  }

  public static final class LightsMode
  {
    public static final int ILLUM_AIM_OFF = 0;
    public static final int AIMER_ONLY = 1;
    public static final int ILLUM_AIM_ON = 5;
    public static final int ILLUM_ONLY = 7;
  }

  public static final class OCRTemplate
  {
    public static final int USER = 1;
    public static final int PASSPORT = 2;
    public static final int ISBN = 4;
    public static final int PRICE_FIELD = 8;
    public static final int MICRE13B = 16;
  }

  public static final class OCRMode
  {
    public static final int OCR_OFF = 0;
    public static final int OCR_NORMAL_VIDEO = 1;
    public static final int OCR_INVERSE = 2;
    public static final int OCR_BOTH = 3;
  }

  public static final class EngineType
  {
    public static final int UNKNOWN = -1;
    public static final int NONE = 0;
    public static final int IMAGER = 1;
    public static final int LASER = 2;
  }

  public static final class EngineID
  {
    public static final int UNKNOWN = -1;
    public static final int NONE = 0;
    public static final int IMAGER_4200_ENGINE = 1;
    public static final int LASER_SE1200_ENGINE = 2;
    public static final int LASER_SE1223_ENGINE = 3;
    public static final int IMAGER_IT4000_ENGINE = 5;
    public static final int IMAGER_IT4100_ENGINE = 6;
    public static final int IMAGER_IT4300_ENGINE = 7;
    public static final int IMAGER_IT5100_ENGINE = 8;
    public static final int IMAGER_IT5300_ENGINE = 9;
    public static final int IMAGER_N5603_ENGINE = 12;
    public static final int IMAGER_N5600_ENGINE = 13;
  }

  public final class SymbologyMask
  {
    public static final int SYM_MASK_FLAGS = 1;
    public static final int SYM_MASK_MIN_LEN = 2;
    public static final int SYM_MASK_MAX_LEN = 4;
    public static final int SYM_MASK_ALL = 7;

    private SymbologyMask()
    {
    }
  }

  public final class SymbologyFlags
  {
    public static final int SYMBOLOGY_ENABLE = 1;
    public static final int SYMBOLOGY_CODABARCONCATENATE = 536870912;
    public static final int SYMBOLOGY_CHECK_ENABLE = 2;
    public static final int SYMBOLOGY_CHECK_TRANSMIT = 4;
    public static final int SYMBOLOGY_START_STOP_XMIT = 8;
    public static final int SYMBOLOGY_ENABLE_APPEND_MODE = 16;
    public static final int SYMBOLOGY_ENABLE_FULLASCII = 32;
    public static final int SYMBOLOGY_NUM_SYS_TRANSMIT = 64;
    public static final int SYMBOLOGY_2_DIGIT_ADDENDA = 128;
    public static final int SYMBOLOGY_5_DIGIT_ADDENDA = 256;
    public static final int SYMBOLOGY_ADDENDA_REQUIRED = 512;
    public static final int SYMBOLOGY_ADDENDA_SEPARATOR = 1024;
    public static final int SYMBOLOGY_EXPANDED_UPCE = 2048;
    public static final int SYMBOLOGY_UPCE1_ENABLE = 4096;
    public static final int SYMBOLOGY_COMPOSITE_UPC = 8192;
    public static final int SYMBOLOGY_AUSTRALIAN_BAR_WIDTH = 65536;
    public static final int SYMBOLOGY_128_APPEND = 524288;
    public static final int SYMBOLOGY_RSE_ENABLE = 8388608;
    public static final int SYMBOLOGY_RSL_ENABLE = 16777216;
    public static final int SYMBOLOGY_RSS_ENABLE = 33554432;
    public static final int SYMBOLOGY_RSX_ENABLE_MASK = 58720256;
    public static final int SYMBOLOGY_TELEPEN_OLD_STYLE = 67108864;
    public static final int SYMBOLOGY_POSICODE_LIMITED_1 = 134217728;
    public static final int SYMBOLOGY_POSICODE_LIMITED_2 = 268435456;

    private SymbologyFlags()
    {
    }
  }

  public static final class SymbologyID
  {
    public static final int SYM_AZTEC = 0;
    public static final int SYM_CODABAR = 1;
    public static final int SYM_CODE11 = 2;
    public static final int SYM_CODE128 = 3;
    public static final int SYM_CODE39 = 4;
    public static final int SYM_CODE49 = 5;
    public static final int SYM_CODE93 = 6;
    public static final int SYM_COMPOSITE = 7;
    public static final int SYM_DATAMATRIX = 8;
    public static final int SYM_EAN8 = 9;
    public static final int SYM_EAN13 = 10;
    public static final int SYM_INT25 = 11;
    public static final int SYM_MAXICODE = 12;
    public static final int SYM_MICROPDF = 13;
    public static final int SYM_OCR = 14;
    public static final int SYM_PDF417 = 15;
    public static final int SYM_POSTNET = 16;
    public static final int SYM_QR = 17;
    public static final int SYM_RSS = 18;
    public static final int SYM_UPCA = 19;
    public static final int SYM_UPCE0 = 20;
    public static final int SYM_UPCE1 = 21;
    public static final int SYM_ISBT = 22;
    public static final int SYM_BPO = 23;
    public static final int SYM_CANPOST = 24;
    public static final int SYM_AUSPOST = 25;
    public static final int SYM_IATA25 = 26;
    public static final int SYM_CODABLOCK = 27;
    public static final int SYM_JAPOST = 28;
    public static final int SYM_PLANET = 29;
    public static final int SYM_DUTCHPOST = 30;
    public static final int SYM_MSI = 31;
    public static final int SYM_TLCODE39 = 32;
    public static final int SYM_TRIOPTIC = 33;
    public static final int SYM_CODE32 = 34;
    public static final int SYM_STRT25 = 35;
    public static final int SYM_MATRIX25 = 36;
    public static final int SYM_PLESSEY = 37;
    public static final int SYM_CHINAPOST = 38;
    public static final int SYM_KOREAPOST = 39;
    public static final int SYM_TELEPEN = 40;
    public static final int SYM_CODE16K = 41;
    public static final int SYM_POSICODE = 42;
    public static final int SYM_COUPONCODE = 43;
    public static final int SYM_USPS4CB = 44;
    public static final int SYM_IDTAG = 45;
    public static final int SYM_LABEL = 46;
    public static final int SYM_GS1_128 = 47;
    public static final int SYM_HANXIN = 48;
    public static final int SYM_GRIDMATRIX = 49;
    public static final int SYM_POSTALS = 50;
    public static final int SYM_US_POSTALS1 = 51;
    public static final int SYMBOLOGIES = 52;
    public static final int SYM_ALL = 100;
  }
}