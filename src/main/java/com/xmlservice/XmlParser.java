package com.xmlservice;

import com.xmlservice.data.CategoryData;
import com.xmlservice.data.CurrencyData;
import com.xmlservice.data.OfferData;
import groovy.xml.XmlSlurper;
import groovy.xml.slurpersupport.GPathResult;
import groovy.xml.slurpersupport.NodeChild;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XmlParser {

    private GPathResult xmlRoot;
    private final String xmlUrl;

    public XmlParser(String xmlUrl) {
        this.xmlUrl = xmlUrl;
    }

    public void loadXml() {
        try (InputStream is = new URL(xmlUrl).openStream()) {
            javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            javax.xml.parsers.SAXParser parser = factory.newSAXParser();
            XmlSlurper xmlSlurper = new XmlSlurper(parser);
            xmlRoot = xmlSlurper.parse(is);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка чтения XML: " + e.getMessage(), e);
        }
    }

    public GPathResult getXmlRoot() {
        if (xmlRoot == null) {
            loadXml();
        }
        return xmlRoot;
    }

    public List<String> getTableNames() {
        List<String> tables = new ArrayList<>();
        GPathResult shop = getShopNode();

        if (shop != null && !shop.isEmpty()) {
            if (hasNode(shop, "currencies")) tables.add("currency");
            if (hasNode(shop, "categories")) tables.add("categories");
            if (hasNode(shop, "offers")) tables.add("offers");
        }

        return tables;
    }

    public GPathResult getShopNode() {
        GPathResult root = getXmlRoot();
        try {
            Object shop = root.getProperty("shop");
            if (shop instanceof GPathResult && !((GPathResult) shop).isEmpty()) {
                return (GPathResult) shop;
            }

            Object ymlCatalog = root.getProperty("yml_catalog");
            if (ymlCatalog instanceof GPathResult) {
                Object catalogShop = ((GPathResult) ymlCatalog).getProperty("shop");
                if (catalogShop instanceof GPathResult && !((GPathResult) catalogShop).isEmpty()) {
                    return (GPathResult) catalogShop;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean hasNode(GPathResult parent, String nodeName) {
        Object node = parent.getProperty(nodeName);
        return node instanceof GPathResult && !((GPathResult) node).isEmpty();
    }

    public List<CurrencyData> parseCurrencies() {
        List<CurrencyData> currencies = new ArrayList<>();
        GPathResult shop = getShopNode();
        if (shop == null) return currencies;

        Object currenciesObj = shop.getProperty("currencies");
        if (!(currenciesObj instanceof GPathResult currenciesNode)) return currencies;

        Object currencyObj = currenciesNode.getProperty("currency");
        if (!(currencyObj instanceof GPathResult currencyNodes)) return currencies;

        for (Object obj : currencyNodes) {
            if (obj instanceof NodeChild currency) {
                String code = extractAttribute(currency, "id");
                String rateStr = extractAttribute(currency, "rate");

                if (code != null && rateStr != null) {
                    currencies.add(new CurrencyData(code.trim(), new BigDecimal(rateStr.trim())));
                }
            }
        }
        return currencies;
    }

    public List<CategoryData> parseCategories() {
        List<CategoryData> categories = new ArrayList<>();
        GPathResult shop = getShopNode();
        if (shop == null) return categories;

        Object categoriesObj = shop.getProperty("categories");
        if (!(categoriesObj instanceof GPathResult categoriesNode)) return categories;

        Object categoryObj = categoriesNode.getProperty("category");
        if (!(categoryObj instanceof GPathResult categoryNodes)) return categories;

        for (Object obj : categoryNodes) {
            if (obj instanceof NodeChild category) {
                String id = extractAttribute(category, "id");
                String name = category.text();

                if (id != null && name != null && !name.isEmpty()) {
                    categories.add(new CategoryData(id.trim(), name.trim()));
                }
            }
        }
        return categories;
    }

    public List<OfferData> parseOffers() {
        List<OfferData> offers = new ArrayList<>();
        GPathResult shop = getShopNode();
        if (shop == null) return offers;

        Object offersObj = shop.getProperty("offers");
        if (!(offersObj instanceof GPathResult offersNode)) return offers;

        Object offerObj = offersNode.getProperty("offer");
        if (!(offerObj instanceof GPathResult offerNodes)) return offers;

        for (Object obj : offerNodes) {
            if (obj instanceof NodeChild offer) {
                String vendorCode = extractAttribute(offer, "id");
                String name = extractChildText(offer, "name");
                String categoryId = extractChildText(offer, "categoryId");
                String priceStr = extractChildText(offer, "price");
                String currencyCode = extractChildText(offer, "currencyId");

                if (vendorCode != null) {
                    offers.add(new OfferData(
                            vendorCode.trim(),
                            name != null ? name.trim() : "",
                            categoryId != null ? categoryId.trim() : null,
                            priceStr != null ? new BigDecimal(priceStr.trim()) : null,
                            currencyCode != null ? currencyCode.trim() : null
                    ));
                }
            }
        }
        return offers;
    }

    private String extractAttribute(NodeChild node, String attrName) {
        try {
            Map<?, ?> attrs = node.attributes();
            if (attrs != null) {
                Object attr = attrs.get(attrName);
                if (attr != null) {
                    return attr.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String extractChildText(NodeChild node, String childName) {
        try {
            Object children = node.getProperty(childName);
            if (children instanceof GPathResult && !((GPathResult) children).isEmpty()) {
                return ((GPathResult) children).text();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}




