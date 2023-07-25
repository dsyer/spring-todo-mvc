@JStacheConfig(using = Application.class, interfacing = @JStacheInterfaces(templateAnnotations = { Component.class }))
package example.todomvc.web;

import org.springframework.stereotype.Component;

import example.todomvc.Application;
import io.jstach.jstache.JStacheConfig;
import io.jstach.jstache.JStacheInterfaces;
