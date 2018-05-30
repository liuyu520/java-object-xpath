/*
 * Created by Roman Kolesnik on 2018-03-14
 */
package com.kunlunsoft.util.objectxpath;

import com.common.util.ReflectHWUtils;
import com.kunlunsoft.util.objectxpath.exception.InvalidXPathExpressionError;
import com.kunlunsoft.util.objectxpath.xpath.*;

import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static com.kunlunsoft.util.objectxpath.Lists.arrayListOf;
import static com.kunlunsoft.util.objectxpath.Lists.transformList;
import static java.util.Collections.emptyList;

public class ObjectXpath {

    private static final List<XPathNodeType> NODE_TYPES = arrayListOf( //order is important here
            XPathNodeType.ROOT_NODE,
            XPathNodeType.PARENT_NODE,
            XPathNodeType.ANY_NODE_WITH_ATTRIBUTE,
            XPathNodeType.ANY_NODE_WITH_INDEX,
            XPathNodeType.NODE_WITH_ATTRIBUTE,
            XPathNodeType.NODE_WITH_INDEX,
            XPathNodeType.ANY_NODE,
            XPathNodeType.SIMPLE_NODE,
            XPathNodeType.NODE_ATTRIBUTE
    );

    /**
     * {@link Set} of wrapper types considered as 'primitive'.
     */
    private static final Set<Class> primitiveTypes = new HashSet<>();

    static {
        primitiveTypes.add(Boolean.class);
        primitiveTypes.add(Character.class);
        primitiveTypes.add(Byte.class);
        primitiveTypes.add(Short.class);
        primitiveTypes.add(Integer.class);
        primitiveTypes.add(Long.class);
        primitiveTypes.add(Float.class);
        primitiveTypes.add(Double.class);
        primitiveTypes.add(String.class);
    }

    //todo add javadoc here
    public Object process(String xPath, Object object) {
        if (object == null) {
            throw new IllegalArgumentException("object cannot be null");
        }
        checkXpathExpression(xPath);
        List<XPathNode> xPathNodes = parseXPath(xPath);
        List<Node> nodes = arrayListOf(objectToTree(object));
        List<Node> result = arrayListOf();
        while (xPathNodes.size() > 0) {
            XPathNode xPathNode = xPathNodes.remove(0);
            boolean lastXpathNode = xPathNodes.isEmpty();
            if (xPathNode.type == XPathNodeType.ROOT_NODE) {
                result.add(nodes.get(0));
                break;
            }
            for (Node node : Lists.removeAll(nodes)) {
                List<Node> foundNodes = findNextNode(node, xPathNode);
                if (foundNodes.size() == 1) {
                    node = foundNodes.get(0);
                    if (xPathNodeWithIndex(xPathNode)) { //one node returned, pick a child with index
                        int index = ((NodeWithIndex) xPathNode).index - 1;
                        Optional<Node> child = getNodeWithIndex(node.children, index);
                        if (child.isPresent()) {
                            nodes.add(child.get());
                            if (lastXpathNode) {
                                result.add(child.get());
                            }
                        }
                        continue;
                    }
                    if (nodeMatchesXpathNode(node, xPathNode)) {
                        nodes.add(node);
                        if (lastXpathNode) {
                            result.add(node);
                        }
                    }
                } else {
                    if (xPathNodeWithIndex(xPathNode)) { //multiple nodes returned, pick a node with index
                        int index = ((NodeWithIndex) xPathNode).index - 1;
                        Optional<Node> _node = getNodeWithIndex(foundNodes, index);
                        if (_node.isPresent()) {
                            nodes.add(_node.get());
                            if (lastXpathNode) {
                                result.add(_node.get());
                            }
                        }
                        continue;
                    }
                    foundNodes.forEach(_node -> {
                        if (nodeMatchesXpathNode(_node, xPathNode)) {
                            nodes.add(_node);
                            if (lastXpathNode) {
                                result.add(_node);
                            }
                        }
                    });
                }
            }
        }
        result.removeIf(node -> node.value == null);
        if (result.size() == 0) {
            return null;
        }
        if (result.size() == 1) {
            return result.get(0).value;
        }
        return result.stream()
                .map(node -> node.value)
                .collect(Collectors.toList());
    }

    /**
     * Finds XPath node in provided XPath string.
     *
     * @param xPath XPath string
     * @return {@link Optional} of {@link XPathNode}
     */
    private Optional<XPathNode> findNextXPathNode(String xPath) {
        for (XPathNodeType nodeType : NODE_TYPES) {
            Matcher matcher = nodeType.pattern.matcher(xPath);
            if (nodeType.matches(matcher)) {
                return Optional.of(nodeType.create(matcher));
            }
        }
        return Optional.empty();
    }

    /**
     * Parses provided XPath string and converts it
     * into a list of {@link XPathNode}.
     *
     * @param xPath XPath string
     * @return list of {@link XPathNode}
     */
    private List<XPathNode> parseXPath(String xPath) {
        List<XPathNode> nodes = new LinkedList<>(); //LinkedList for better 'remove' performance
        Optional<XPathNode> node_;
        while ((node_ = findNextXPathNode(xPath)).isPresent()) {
            XPathNode node = node_.get();
            nodes.add(node);
            xPath = xPath.substring(node.endIndex);
        }
        return nodes;
    }

    private boolean nodeHasAttribute(Node node, String attrName, Object attrValue) {
        return node.value != null && getFieldValue(attrName, node.value)
                .filter(attr -> Objects.equals(attrValue, attr)).isPresent();
    }

    private Optional<Node> getNodeWithIndex(List<Node> nodes, int index) {
        if (nodes.size() == 0) {
            return Optional.empty();
        }
        if (index >= nodes.size()) {
            return Optional.empty();
        }
        return Optional.of(nodes.get(index));
    }

    private boolean xPathNodeWithIndex(XPathNode xPathNode) {
        return xPathNode.type == XPathNodeType.ANY_NODE_WITH_INDEX || xPathNode.type == XPathNodeType.NODE_WITH_INDEX;
    }

    private boolean xPathNodeWithAttribute(XPathNode xPathNode) {
        return xPathNode.type == XPathNodeType.ANY_NODE_WITH_ATTRIBUTE || xPathNode.type == XPathNodeType.NODE_WITH_ATTRIBUTE;
    }

    private boolean nodeMatchesXpathNode(Node node, XPathNode xPathNode) {
        if (xPathNodeWithAttribute(xPathNode)) {
            NodeWithAttribute nodeWithAttribute = (NodeWithAttribute) xPathNode;
            return nodeHasAttribute(node, nodeWithAttribute.attrName, nodeWithAttribute.attrValue);
        }
        return true;
    }

    private Optional<String> getFieldValue(String fieldName, Object holder) {
        Object value = ReflectHWUtils.getObjectValue(holder, fieldName);// ReflectionUtils.getFieldValue(fieldName, holder);
        return Optional.ofNullable(string(value));
//        } catch (FieldNotFoundError fieldNotFoundError) {
//            return Optional.empty();
//        }
    }

    private void checkXpathExpression(String expression) throws InvalidXPathExpressionError {
        try {
            XPathFactory.newInstance().newXPath().compile(expression);
        } catch (XPathExpressionException e) {
            throw new InvalidXPathExpressionError(expression);
        }
    }

    private List<Node> findAttributeNode(Node node, XPathNode xPathNode) {
        List<Node> nodes = arrayListOf();
        for (Node attr : node.attributes) {
            if (attr.name.equals(xPathNode.name)) {
                nodes.add(attr);
            }
        }
        if (xPathNode.relationship == NodeRelationship.DESCENDANT) {
            node.children.forEach(child -> {
                nodes.addAll(findAttributeNode(child, xPathNode));
            });
        }
        return nodes;
    }

    private List<Node> findNextNode(Node parent, XPathNode xPathNode) {
        if (xPathNode.type == XPathNodeType.PARENT_NODE) {
            return parent.parent == null ? emptyList() : arrayListOf(parent.parent);
        }
        if (xPathNode.type == XPathNodeType.NODE_ATTRIBUTE) {
            return findAttributeNode(parent, xPathNode);
        }
        List<Node> nodes = arrayListOf();
        for (Node child : parent.children) {
            if (anyXpathNode(xPathNode)) {
                nodes.add(child);
            } else if (child.name.equals(xPathNode.name)) {
                nodes.add(child);
            }
            if (xPathNode.relationship == NodeRelationship.DESCENDANT) {
                nodes.addAll(findNextNode(child, xPathNode));
            }
        }
        return nodes;
    }

    private boolean anyXpathNode(XPathNode xPathNode) {
        return xPathNode.type == XPathNodeType.ANY_NODE_WITH_ATTRIBUTE
                || xPathNode.type == XPathNodeType.ANY_NODE_WITH_INDEX
                || xPathNode.type == XPathNodeType.ANY_NODE;
    }

    /**
     * Creates a 'tree' from an object.
     *
     * @param object an object for which a tree will created
     * @return tree root node
     */
    private Node objectToTree(Object object) {
        Node root = new Node();
        root.name = getNameFor(object);
        root.value = object;
        root.path = "/" + root.name;
        if (isPrimitive(object)) {
            return root;
        }
        if (ReflectHWUtils.isMap(object)) {
            processMapNode(root);
            return root;
        }
        if (ReflectHWUtils.isCollection(object)) {
            processCollectionNode(root);
            return root;
        }
        processNodeFields(ReflectHWUtils.getAllFieldList(object.getClass()), root);
        return root;
    }

    private Node toNode(Node parent, Object value, String name, Integer index) {
        Node node = new Node();
        node.name = name;
        node.value = value;
        node.path = index == null
                ? parent.path + "/" + name
                : parent.path + "/" + name + "[" + index + "]";
        node.parent = parent;
        if (null == value || isPrimitive(value)) {
            return node;
        }
        if (ReflectHWUtils.isMap(value)) {
            processMapNode(node);
            return node;
        }
        if (ReflectHWUtils.isCollection(value)) {
            processCollectionNode(node);
            return node;
        }
        processNodeFields(ReflectHWUtils.getAllFieldList(value.getClass()), node);
        return node;
    }

    private void processCollectionNode(Node node) {
        Collection collection = (Collection) node.value;
        if (collection.size() > 0) {
            node.children = new ArrayList<>(collection.size());
        }
        int i = 1;
        for (Object item : collection) {
            if (item != null) {
                node.children.add(toNode(node, item, getNameFor(item), i++));
            }
        }
    }

    private void processMapNode(Node node) {
        //todo map key type (string or number)
        Map map = (Map) node.value;
        if (map.size() > 0) {
            node.children = new ArrayList<>(map.size());
        }
        //todo do not use stream here for performance reasons? (iterator instead)
        map.keySet().stream().findFirst().ifPresent(key -> {
            if (isPrimitive(key)) {
                map.entrySet().forEach(entry -> {
                    Map.Entry<Comparable, Object> _entry = (Map.Entry<Comparable, Object>) entry;
                    node.children.add(toNode(node, _entry.getValue(), string(_entry.getKey()), null));
                });
            }
        });
    }

    private void processNodeFields(List<Field> fields, Node node) {
        List<Field> attributes = arrayListOf();
        fields.removeIf(field -> {
            if (isPrimitive(field)) {
                attributes.add(field);
                return true;
            }
            return false;
        });
        node.attributes = transformList(attributes, field
                -> toNode(node, ReflectHWUtils.getObjectValue(node.value, field), field.getName(), null));
        node.children = transformList(fields, field
                -> toNode(node, ReflectHWUtils.getObjectValue(node.value, field), field.getName(), null));
    }

    //TODO allow to customize primitive types
    private static boolean isPrimitive(Field field) {
        Class type = field.getType();
        return type.isPrimitive() || primitiveTypes.contains(type);
    }

    //TODO allow to customize primitive types
    private static boolean isPrimitive(Object object) {
        Class type = object.getClass();
        return type.isPrimitive() || primitiveTypes.contains(type);
    }

    /**
     * Gets a name for an object based on it's class name.
     */
    private String getNameFor(Object object) {
        String name = object.getClass().getSimpleName();
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    /**
     * Converts some object to a string.
     */
    private String string(Object object) {
        return object == null ? null : String.valueOf(object);
    }

    /**
     * Represents object tree node.
     */
    private class Node {
        Node parent;
        String name;
        String path;
        Object value;
        List<Node> attributes = emptyList();
        List<Node> children = emptyList();

        @Override
        public String toString() {
            return path;
        }
    }
}
