package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the {@code libs} extension.
 */
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final ComLibraryAccessors laccForComLibraryAccessors = new ComLibraryAccessors(owner);
    private final IoLibraryAccessors laccForIoLibraryAccessors = new IoLibraryAccessors(owner);
    private final OrgLibraryAccessors laccForOrgLibraryAccessors = new OrgLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

    /**
     * Group of libraries at <b>com</b>
     */
    public ComLibraryAccessors getCom() {
        return laccForComLibraryAccessors;
    }

    /**
     * Group of libraries at <b>io</b>
     */
    public IoLibraryAccessors getIo() {
        return laccForIoLibraryAccessors;
    }

    /**
     * Group of libraries at <b>org</b>
     */
    public OrgLibraryAccessors getOrg() {
        return laccForOrgLibraryAccessors;
    }

    /**
     * Group of versions at <b>versions</b>
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Group of bundles at <b>bundles</b>
     */
    public BundleAccessors getBundles() {
        return baccForBundleAccessors;
    }

    /**
     * Group of plugins at <b>plugins</b>
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class ComLibraryAccessors extends SubDependencyFactory {
        private final ComItextpdfLibraryAccessors laccForComItextpdfLibraryAccessors = new ComItextpdfLibraryAccessors(owner);
        private final ComJoacademyLibraryAccessors laccForComJoacademyLibraryAccessors = new ComJoacademyLibraryAccessors(owner);
        private final ComSunLibraryAccessors laccForComSunLibraryAccessors = new ComSunLibraryAccessors(owner);

        public ComLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.itextpdf</b>
         */
        public ComItextpdfLibraryAccessors getItextpdf() {
            return laccForComItextpdfLibraryAccessors;
        }

        /**
         * Group of libraries at <b>com.joacademy</b>
         */
        public ComJoacademyLibraryAccessors getJoacademy() {
            return laccForComJoacademyLibraryAccessors;
        }

        /**
         * Group of libraries at <b>com.sun</b>
         */
        public ComSunLibraryAccessors getSun() {
            return laccForComSunLibraryAccessors;
        }

    }

    public static class ComItextpdfLibraryAccessors extends SubDependencyFactory {
        private final ComItextpdfItext7LibraryAccessors laccForComItextpdfItext7LibraryAccessors = new ComItextpdfItext7LibraryAccessors(owner);

        public ComItextpdfLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.itextpdf.itext7</b>
         */
        public ComItextpdfItext7LibraryAccessors getItext7() {
            return laccForComItextpdfItext7LibraryAccessors;
        }

    }

    public static class ComItextpdfItext7LibraryAccessors extends SubDependencyFactory {

        public ComItextpdfItext7LibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>com.itextpdf:itext7-core</b> coordinates and
         * with version reference <b>com.itextpdf.itext7.core</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("com.itextpdf.itext7.core");
        }

    }

    public static class ComJoacademyLibraryAccessors extends SubDependencyFactory {

        public ComJoacademyLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>automationframework</b> with <b>com.joacademy:AutomationFramework</b> coordinates and
         * with version reference <b>com.joacademy.automationframework</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAutomationframework() {
            return create("com.joacademy.automationframework");
        }

    }

    public static class ComSunLibraryAccessors extends SubDependencyFactory {
        private final ComSunMailLibraryAccessors laccForComSunMailLibraryAccessors = new ComSunMailLibraryAccessors(owner);

        public ComSunLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.sun.mail</b>
         */
        public ComSunMailLibraryAccessors getMail() {
            return laccForComSunMailLibraryAccessors;
        }

    }

    public static class ComSunMailLibraryAccessors extends SubDependencyFactory {
        private final ComSunMailJavaxLibraryAccessors laccForComSunMailJavaxLibraryAccessors = new ComSunMailJavaxLibraryAccessors(owner);

        public ComSunMailLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.sun.mail.javax</b>
         */
        public ComSunMailJavaxLibraryAccessors getJavax() {
            return laccForComSunMailJavaxLibraryAccessors;
        }

    }

    public static class ComSunMailJavaxLibraryAccessors extends SubDependencyFactory {

        public ComSunMailJavaxLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>mail</b> with <b>com.sun.mail:javax.mail</b> coordinates and
         * with version reference <b>com.sun.mail.javax.mail</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getMail() {
            return create("com.sun.mail.javax.mail");
        }

    }

    public static class IoLibraryAccessors extends SubDependencyFactory {
        private final IoQametaLibraryAccessors laccForIoQametaLibraryAccessors = new IoQametaLibraryAccessors(owner);
        private final IoRestLibraryAccessors laccForIoRestLibraryAccessors = new IoRestLibraryAccessors(owner);

        public IoLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>io.qameta</b>
         */
        public IoQametaLibraryAccessors getQameta() {
            return laccForIoQametaLibraryAccessors;
        }

        /**
         * Group of libraries at <b>io.rest</b>
         */
        public IoRestLibraryAccessors getRest() {
            return laccForIoRestLibraryAccessors;
        }

    }

    public static class IoQametaLibraryAccessors extends SubDependencyFactory {
        private final IoQametaAllureLibraryAccessors laccForIoQametaAllureLibraryAccessors = new IoQametaAllureLibraryAccessors(owner);

        public IoQametaLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>io.qameta.allure</b>
         */
        public IoQametaAllureLibraryAccessors getAllure() {
            return laccForIoQametaAllureLibraryAccessors;
        }

    }

    public static class IoQametaAllureLibraryAccessors extends SubDependencyFactory {
        private final IoQametaAllureAllureLibraryAccessors laccForIoQametaAllureAllureLibraryAccessors = new IoQametaAllureAllureLibraryAccessors(owner);

        public IoQametaAllureLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>io.qameta.allure.allure</b>
         */
        public IoQametaAllureAllureLibraryAccessors getAllure() {
            return laccForIoQametaAllureAllureLibraryAccessors;
        }

    }

    public static class IoQametaAllureAllureLibraryAccessors extends SubDependencyFactory {
        private final IoQametaAllureAllureJavaLibraryAccessors laccForIoQametaAllureAllureJavaLibraryAccessors = new IoQametaAllureAllureJavaLibraryAccessors(owner);
        private final IoQametaAllureAllureRestLibraryAccessors laccForIoQametaAllureAllureRestLibraryAccessors = new IoQametaAllureAllureRestLibraryAccessors(owner);

        public IoQametaAllureAllureLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>testng</b> with <b>io.qameta.allure:allure-testng</b> coordinates and
         * with version reference <b>io.qameta.allure.allure.testng</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTestng() {
            return create("io.qameta.allure.allure.testng");
        }

        /**
         * Group of libraries at <b>io.qameta.allure.allure.java</b>
         */
        public IoQametaAllureAllureJavaLibraryAccessors getJava() {
            return laccForIoQametaAllureAllureJavaLibraryAccessors;
        }

        /**
         * Group of libraries at <b>io.qameta.allure.allure.rest</b>
         */
        public IoQametaAllureAllureRestLibraryAccessors getRest() {
            return laccForIoQametaAllureAllureRestLibraryAccessors;
        }

    }

    public static class IoQametaAllureAllureJavaLibraryAccessors extends SubDependencyFactory {

        public IoQametaAllureAllureJavaLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>commons</b> with <b>io.qameta.allure:allure-java-commons</b> coordinates and
         * with version reference <b>io.qameta.allure.allure.java.commons</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCommons() {
            return create("io.qameta.allure.allure.java.commons");
        }

    }

    public static class IoQametaAllureAllureRestLibraryAccessors extends SubDependencyFactory {

        public IoQametaAllureAllureRestLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>assured</b> with <b>io.qameta.allure:allure-rest-assured</b> coordinates and
         * with version reference <b>io.qameta.allure.allure.rest.assured</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAssured() {
            return create("io.qameta.allure.allure.rest.assured");
        }

    }

    public static class IoRestLibraryAccessors extends SubDependencyFactory {
        private final IoRestAssuredLibraryAccessors laccForIoRestAssuredLibraryAccessors = new IoRestAssuredLibraryAccessors(owner);

        public IoRestLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>io.rest.assured</b>
         */
        public IoRestAssuredLibraryAccessors getAssured() {
            return laccForIoRestAssuredLibraryAccessors;
        }

    }

    public static class IoRestAssuredLibraryAccessors extends SubDependencyFactory {
        private final IoRestAssuredRestLibraryAccessors laccForIoRestAssuredRestLibraryAccessors = new IoRestAssuredRestLibraryAccessors(owner);

        public IoRestAssuredLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>io.rest.assured.rest</b>
         */
        public IoRestAssuredRestLibraryAccessors getRest() {
            return laccForIoRestAssuredRestLibraryAccessors;
        }

    }

    public static class IoRestAssuredRestLibraryAccessors extends SubDependencyFactory {

        public IoRestAssuredRestLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>assured</b> with <b>io.rest-assured:rest-assured</b> coordinates and
         * with version reference <b>io.rest.assured.rest.assured</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAssured() {
            return create("io.rest.assured.rest.assured");
        }

    }

    public static class OrgLibraryAccessors extends SubDependencyFactory {
        private final OrgApacheLibraryAccessors laccForOrgApacheLibraryAccessors = new OrgApacheLibraryAccessors(owner);
        private final OrgSlf4jLibraryAccessors laccForOrgSlf4jLibraryAccessors = new OrgSlf4jLibraryAccessors(owner);
        private final OrgTestngLibraryAccessors laccForOrgTestngLibraryAccessors = new OrgTestngLibraryAccessors(owner);
        private final OrgYamlLibraryAccessors laccForOrgYamlLibraryAccessors = new OrgYamlLibraryAccessors(owner);

        public OrgLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.apache</b>
         */
        public OrgApacheLibraryAccessors getApache() {
            return laccForOrgApacheLibraryAccessors;
        }

        /**
         * Group of libraries at <b>org.slf4j</b>
         */
        public OrgSlf4jLibraryAccessors getSlf4j() {
            return laccForOrgSlf4jLibraryAccessors;
        }

        /**
         * Group of libraries at <b>org.testng</b>
         */
        public OrgTestngLibraryAccessors getTestng() {
            return laccForOrgTestngLibraryAccessors;
        }

        /**
         * Group of libraries at <b>org.yaml</b>
         */
        public OrgYamlLibraryAccessors getYaml() {
            return laccForOrgYamlLibraryAccessors;
        }

    }

    public static class OrgApacheLibraryAccessors extends SubDependencyFactory {
        private final OrgApacheCommonsLibraryAccessors laccForOrgApacheCommonsLibraryAccessors = new OrgApacheCommonsLibraryAccessors(owner);

        public OrgApacheLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.apache.commons</b>
         */
        public OrgApacheCommonsLibraryAccessors getCommons() {
            return laccForOrgApacheCommonsLibraryAccessors;
        }

    }

    public static class OrgApacheCommonsLibraryAccessors extends SubDependencyFactory {
        private final OrgApacheCommonsCommonsLibraryAccessors laccForOrgApacheCommonsCommonsLibraryAccessors = new OrgApacheCommonsCommonsLibraryAccessors(owner);

        public OrgApacheCommonsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.apache.commons.commons</b>
         */
        public OrgApacheCommonsCommonsLibraryAccessors getCommons() {
            return laccForOrgApacheCommonsCommonsLibraryAccessors;
        }

    }

    public static class OrgApacheCommonsCommonsLibraryAccessors extends SubDependencyFactory {

        public OrgApacheCommonsCommonsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>compress</b> with <b>org.apache.commons:commons-compress</b> coordinates and
         * with version reference <b>org.apache.commons.commons.compress</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCompress() {
            return create("org.apache.commons.commons.compress");
        }

    }

    public static class OrgSlf4jLibraryAccessors extends SubDependencyFactory {
        private final OrgSlf4jSlf4jLibraryAccessors laccForOrgSlf4jSlf4jLibraryAccessors = new OrgSlf4jSlf4jLibraryAccessors(owner);

        public OrgSlf4jLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.slf4j.slf4j</b>
         */
        public OrgSlf4jSlf4jLibraryAccessors getSlf4j() {
            return laccForOrgSlf4jSlf4jLibraryAccessors;
        }

    }

    public static class OrgSlf4jSlf4jLibraryAccessors extends SubDependencyFactory {

        public OrgSlf4jSlf4jLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>simple</b> with <b>org.slf4j:slf4j-simple</b> coordinates and
         * with version reference <b>org.slf4j.slf4j.simple</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSimple() {
            return create("org.slf4j.slf4j.simple");
        }

    }

    public static class OrgTestngLibraryAccessors extends SubDependencyFactory {

        public OrgTestngLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>testng</b> with <b>org.testng:testng</b> coordinates and
         * with version reference <b>org.testng.testng</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTestng() {
            return create("org.testng.testng");
        }

    }

    public static class OrgYamlLibraryAccessors extends SubDependencyFactory {

        public OrgYamlLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>snakeyaml</b> with <b>org.yaml:snakeyaml</b> coordinates and
         * with version reference <b>org.yaml.snakeyaml</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSnakeyaml() {
            return create("org.yaml.snakeyaml");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        private final ComVersionAccessors vaccForComVersionAccessors = new ComVersionAccessors(providers, config);
        private final IoVersionAccessors vaccForIoVersionAccessors = new IoVersionAccessors(providers, config);
        private final OrgVersionAccessors vaccForOrgVersionAccessors = new OrgVersionAccessors(providers, config);
        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com</b>
         */
        public ComVersionAccessors getCom() {
            return vaccForComVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.io</b>
         */
        public IoVersionAccessors getIo() {
            return vaccForIoVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org</b>
         */
        public OrgVersionAccessors getOrg() {
            return vaccForOrgVersionAccessors;
        }

    }

    public static class ComVersionAccessors extends VersionFactory  {

        private final ComItextpdfVersionAccessors vaccForComItextpdfVersionAccessors = new ComItextpdfVersionAccessors(providers, config);
        private final ComJoacademyVersionAccessors vaccForComJoacademyVersionAccessors = new ComJoacademyVersionAccessors(providers, config);
        private final ComSunVersionAccessors vaccForComSunVersionAccessors = new ComSunVersionAccessors(providers, config);
        public ComVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.itextpdf</b>
         */
        public ComItextpdfVersionAccessors getItextpdf() {
            return vaccForComItextpdfVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.com.joacademy</b>
         */
        public ComJoacademyVersionAccessors getJoacademy() {
            return vaccForComJoacademyVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.com.sun</b>
         */
        public ComSunVersionAccessors getSun() {
            return vaccForComSunVersionAccessors;
        }

    }

    public static class ComItextpdfVersionAccessors extends VersionFactory  {

        private final ComItextpdfItext7VersionAccessors vaccForComItextpdfItext7VersionAccessors = new ComItextpdfItext7VersionAccessors(providers, config);
        public ComItextpdfVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.itextpdf.itext7</b>
         */
        public ComItextpdfItext7VersionAccessors getItext7() {
            return vaccForComItextpdfItext7VersionAccessors;
        }

    }

    public static class ComItextpdfItext7VersionAccessors extends VersionFactory  {

        public ComItextpdfItext7VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.itextpdf.itext7.core</b> with value <b>7.2.3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getCore() { return getVersion("com.itextpdf.itext7.core"); }

    }

    public static class ComJoacademyVersionAccessors extends VersionFactory  {

        public ComJoacademyVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.joacademy.automationframework</b> with value <b>0.0.1-SNAPSHOT</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getAutomationframework() { return getVersion("com.joacademy.automationframework"); }

    }

    public static class ComSunVersionAccessors extends VersionFactory  {

        private final ComSunMailVersionAccessors vaccForComSunMailVersionAccessors = new ComSunMailVersionAccessors(providers, config);
        public ComSunVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.sun.mail</b>
         */
        public ComSunMailVersionAccessors getMail() {
            return vaccForComSunMailVersionAccessors;
        }

    }

    public static class ComSunMailVersionAccessors extends VersionFactory  {

        private final ComSunMailJavaxVersionAccessors vaccForComSunMailJavaxVersionAccessors = new ComSunMailJavaxVersionAccessors(providers, config);
        public ComSunMailVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.sun.mail.javax</b>
         */
        public ComSunMailJavaxVersionAccessors getJavax() {
            return vaccForComSunMailJavaxVersionAccessors;
        }

    }

    public static class ComSunMailJavaxVersionAccessors extends VersionFactory  {

        public ComSunMailJavaxVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.sun.mail.javax.mail</b> with value <b>1.6.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getMail() { return getVersion("com.sun.mail.javax.mail"); }

    }

    public static class IoVersionAccessors extends VersionFactory  {

        private final IoQametaVersionAccessors vaccForIoQametaVersionAccessors = new IoQametaVersionAccessors(providers, config);
        private final IoRestVersionAccessors vaccForIoRestVersionAccessors = new IoRestVersionAccessors(providers, config);
        public IoVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.io.qameta</b>
         */
        public IoQametaVersionAccessors getQameta() {
            return vaccForIoQametaVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.io.rest</b>
         */
        public IoRestVersionAccessors getRest() {
            return vaccForIoRestVersionAccessors;
        }

    }

    public static class IoQametaVersionAccessors extends VersionFactory  {

        private final IoQametaAllureVersionAccessors vaccForIoQametaAllureVersionAccessors = new IoQametaAllureVersionAccessors(providers, config);
        public IoQametaVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.io.qameta.allure</b>
         */
        public IoQametaAllureVersionAccessors getAllure() {
            return vaccForIoQametaAllureVersionAccessors;
        }

    }

    public static class IoQametaAllureVersionAccessors extends VersionFactory  {

        private final IoQametaAllureAllureVersionAccessors vaccForIoQametaAllureAllureVersionAccessors = new IoQametaAllureAllureVersionAccessors(providers, config);
        public IoQametaAllureVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.io.qameta.allure.allure</b>
         */
        public IoQametaAllureAllureVersionAccessors getAllure() {
            return vaccForIoQametaAllureAllureVersionAccessors;
        }

    }

    public static class IoQametaAllureAllureVersionAccessors extends VersionFactory  {

        private final IoQametaAllureAllureJavaVersionAccessors vaccForIoQametaAllureAllureJavaVersionAccessors = new IoQametaAllureAllureJavaVersionAccessors(providers, config);
        private final IoQametaAllureAllureRestVersionAccessors vaccForIoQametaAllureAllureRestVersionAccessors = new IoQametaAllureAllureRestVersionAccessors(providers, config);
        public IoQametaAllureAllureVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>io.qameta.allure.allure.testng</b> with value <b>2.20.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getTestng() { return getVersion("io.qameta.allure.allure.testng"); }

        /**
         * Group of versions at <b>versions.io.qameta.allure.allure.java</b>
         */
        public IoQametaAllureAllureJavaVersionAccessors getJava() {
            return vaccForIoQametaAllureAllureJavaVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.io.qameta.allure.allure.rest</b>
         */
        public IoQametaAllureAllureRestVersionAccessors getRest() {
            return vaccForIoQametaAllureAllureRestVersionAccessors;
        }

    }

    public static class IoQametaAllureAllureJavaVersionAccessors extends VersionFactory  {

        public IoQametaAllureAllureJavaVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>io.qameta.allure.allure.java.commons</b> with value <b>2.24.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getCommons() { return getVersion("io.qameta.allure.allure.java.commons"); }

    }

    public static class IoQametaAllureAllureRestVersionAccessors extends VersionFactory  {

        public IoQametaAllureAllureRestVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>io.qameta.allure.allure.rest.assured</b> with value <b>2.24.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getAssured() { return getVersion("io.qameta.allure.allure.rest.assured"); }

    }

    public static class IoRestVersionAccessors extends VersionFactory  {

        private final IoRestAssuredVersionAccessors vaccForIoRestAssuredVersionAccessors = new IoRestAssuredVersionAccessors(providers, config);
        public IoRestVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.io.rest.assured</b>
         */
        public IoRestAssuredVersionAccessors getAssured() {
            return vaccForIoRestAssuredVersionAccessors;
        }

    }

    public static class IoRestAssuredVersionAccessors extends VersionFactory  {

        private final IoRestAssuredRestVersionAccessors vaccForIoRestAssuredRestVersionAccessors = new IoRestAssuredRestVersionAccessors(providers, config);
        public IoRestAssuredVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.io.rest.assured.rest</b>
         */
        public IoRestAssuredRestVersionAccessors getRest() {
            return vaccForIoRestAssuredRestVersionAccessors;
        }

    }

    public static class IoRestAssuredRestVersionAccessors extends VersionFactory  {

        public IoRestAssuredRestVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>io.rest.assured.rest.assured</b> with value <b>5.3.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getAssured() { return getVersion("io.rest.assured.rest.assured"); }

    }

    public static class OrgVersionAccessors extends VersionFactory  {

        private final OrgApacheVersionAccessors vaccForOrgApacheVersionAccessors = new OrgApacheVersionAccessors(providers, config);
        private final OrgSlf4jVersionAccessors vaccForOrgSlf4jVersionAccessors = new OrgSlf4jVersionAccessors(providers, config);
        private final OrgTestngVersionAccessors vaccForOrgTestngVersionAccessors = new OrgTestngVersionAccessors(providers, config);
        private final OrgYamlVersionAccessors vaccForOrgYamlVersionAccessors = new OrgYamlVersionAccessors(providers, config);
        public OrgVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.apache</b>
         */
        public OrgApacheVersionAccessors getApache() {
            return vaccForOrgApacheVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org.slf4j</b>
         */
        public OrgSlf4jVersionAccessors getSlf4j() {
            return vaccForOrgSlf4jVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org.testng</b>
         */
        public OrgTestngVersionAccessors getTestng() {
            return vaccForOrgTestngVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org.yaml</b>
         */
        public OrgYamlVersionAccessors getYaml() {
            return vaccForOrgYamlVersionAccessors;
        }

    }

    public static class OrgApacheVersionAccessors extends VersionFactory  {

        private final OrgApacheCommonsVersionAccessors vaccForOrgApacheCommonsVersionAccessors = new OrgApacheCommonsVersionAccessors(providers, config);
        public OrgApacheVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.apache.commons</b>
         */
        public OrgApacheCommonsVersionAccessors getCommons() {
            return vaccForOrgApacheCommonsVersionAccessors;
        }

    }

    public static class OrgApacheCommonsVersionAccessors extends VersionFactory  {

        private final OrgApacheCommonsCommonsVersionAccessors vaccForOrgApacheCommonsCommonsVersionAccessors = new OrgApacheCommonsCommonsVersionAccessors(providers, config);
        public OrgApacheCommonsVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.apache.commons.commons</b>
         */
        public OrgApacheCommonsCommonsVersionAccessors getCommons() {
            return vaccForOrgApacheCommonsCommonsVersionAccessors;
        }

    }

    public static class OrgApacheCommonsCommonsVersionAccessors extends VersionFactory  {

        public OrgApacheCommonsCommonsVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.apache.commons.commons.compress</b> with value <b>1.26.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getCompress() { return getVersion("org.apache.commons.commons.compress"); }

    }

    public static class OrgSlf4jVersionAccessors extends VersionFactory  {

        private final OrgSlf4jSlf4jVersionAccessors vaccForOrgSlf4jSlf4jVersionAccessors = new OrgSlf4jSlf4jVersionAccessors(providers, config);
        public OrgSlf4jVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.slf4j.slf4j</b>
         */
        public OrgSlf4jSlf4jVersionAccessors getSlf4j() {
            return vaccForOrgSlf4jSlf4jVersionAccessors;
        }

    }

    public static class OrgSlf4jSlf4jVersionAccessors extends VersionFactory  {

        public OrgSlf4jSlf4jVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.slf4j.slf4j.simple</b> with value <b>1.7.36</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSimple() { return getVersion("org.slf4j.slf4j.simple"); }

    }

    public static class OrgTestngVersionAccessors extends VersionFactory  {

        public OrgTestngVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.testng.testng</b> with value <b>6.14.3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getTestng() { return getVersion("org.testng.testng"); }

    }

    public static class OrgYamlVersionAccessors extends VersionFactory  {

        public OrgYamlVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.yaml.snakeyaml</b> with value <b>2.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSnakeyaml() { return getVersion("org.yaml.snakeyaml"); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

    }

    public static class PluginAccessors extends PluginFactory {

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

    }

}
