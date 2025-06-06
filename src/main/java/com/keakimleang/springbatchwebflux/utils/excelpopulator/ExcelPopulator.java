package com.keakimleang.springbatchwebflux.utils.excelpopulator;

import java.io.*;
import org.apache.poi.ss.usermodel.*;

public interface ExcelPopulator {

    Sheet populate();

    ByteArrayOutputStream export();

}
