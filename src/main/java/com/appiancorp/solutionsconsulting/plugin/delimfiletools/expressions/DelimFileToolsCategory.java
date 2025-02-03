package com.appiancorp.solutionsconsulting.plugin.delimfiletools.expressions;

import com.appiancorp.suiteapi.expression.annotations.Category;

import java.lang.annotation.*;

@Category("DelimFileToolsCategory")
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DelimFileToolsCategory {

}

