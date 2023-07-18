package com.chestnut.scan;

import com.chestnut.imported.LocalDateConfiguration;
import com.chestnut.imported.ZonedDateConfiguration;
import com.chestnut.spring.annotation.ComponentScan;
import com.chestnut.spring.annotation.Import;

@ComponentScan
@Import({ LocalDateConfiguration.class, ZonedDateConfiguration.class })
public class ScanApplication {

}
