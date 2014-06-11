package com.vmware.bdd.software.mgmt.plugin.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should be used before configure a cluster, to make sure infrastructure level's 
 * configuration is finished
 * @author line
 *
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface BeforeClusterConfiguration {

}
