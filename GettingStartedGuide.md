# Getting Started Guide #

For maven users, the easiest way to get going is to add the following dependency to your project. Obviously double check that the version is correct in case this documentation ever gets out of sync with the latest release:

```
<dependency>
   <groupId>com.googlecode.simpleobjectassembler</groupId>
   <artifactId>simpleobjectassembler</artifactId>
   <version>1.0.0</version>
</dependency> 
```

Once your project is correctly configured with the Simple Object Assembler on your classpath, add a bean definition to your spring application context for the object assembler:

```
<bean id="objectAssembler" class="com.googlecode.simpleobjectassembler.SimpleObjectAssembler" />
```

If you would like to have the object assembler automatically attempt to convert objects that don't have an explicit converter registered, you can set the assembler property 'automapWhenNoConverterFound' to true;


```
<bean id="objectAssembler" class="com.googlecode.simpleobjectassembler.SimpleObjectAssembler">
  <property name="automapWhenNoConverterFound" value="true"/>
</bean>
```


Now you're ready to use the object assembler with any Converters you write.

## 3rd Party Dependencies ##
I've tried to make most of the dependencies optional or specified them with a scope of 'provided' in the maven dependency configuration.  This way you don't have to worry about conflicting versions of Spring for example.  There is a dependency on Spring 3.x and you will get less configuration if you have annotation based configuration enabled.  It is important to note that if you're downloading and installing the compiled jar in your local m2 repository rather than building from source using maven, you will not get any of the transitive dependencies. If you intend to use annotation based configuration (recommended), then ensure you have jsr250-api in your pom:

```
<dependency>
   <groupId>javax.annotation</groupId>
   <artifactId>jsr250-api</artifactId>
   <version>1.0</version>
</dependency>
```

For anyone not using annotation based configuration, each converter will need an `init-method` to be defined on the bean definition that calls `postConstruct()`, or will need to implement `InitializingBean` and explicitly call `postConstruct()`.  Converters also need an instance of the `SimpleObjectAssembler` to be injected via the setter.  Annotation based projects can skip all this.


## Object Assembly / Conversion ##

The object assembler is the entry point to object conversion. It maintains a registry of Object Converters that are written by the application developer (that's you) to convert one object to another. The registry enables a single converter to exist for converting one type to another.  At this stage this is a conscious design decision, pending change if a case comes for allowing more. So far so good.

When you want to convert an object and some or all of it's contents to another type you generally call the object assembler like this:

```
Destination destination = objectAssembler.assemble(sourceObject, Destination.class);
```

This will look up it's registry for a converter with the same source and destination combination. A converter in it's simplest form might look something like this:

```
public class SourceToDestinationTestObjectConverter extends    AbstractObjectConverter<StaffMember, StaffMemberDto> {

}
```

The assembler uses the Parameterized type values of 'StaffMember' and 'StaffMemberDto' in order to register the converter against those source and destination object types.

You'll notice that this converter extends the `AbstractObjectConverter` which has or defers most of the conversion logic to supporting classes. The end result of calling this converter via the assembler as shown above is that all properties of the same name will be converted from the source to destination object. If two properties with the same name but different types are encountered, the registry will be consulted to lookup a converter with the required source / destination types. The assembler will recursively call itself until all properties are converted.


## Selective Conversion ##

If you want to exclude any part of the object graph from automatically being converted, you can do so in three ways.

  1. By overriding the `disableAutoMapping()` method on your converter implementation and returning `true`
  1. By overriding the `alwaysIgnoreProperties()` method on your converter and returning an IgnoreSet of field names to exclude from conversion
  1. By passing an array or var-args of property names to exclude into the assembler


The first two approaches are useful when you want this behavior for every invocation of the converter, but if you want to be able to specify what to convert upon invocation, the third approach is much more flexible.

For Example:

```
Destination destination objectAssembler.assemble(sourceObject, Destination.class, "ignoreFieldA", "ignoreFieldB");
```

The ignore field paths are aware of nested object graphs so it's possible to ignore any part of the graph, no matter how deep.

```
Destination destination = objectAssembler.assemble(sourceObject, Destination.class, "ignoreCollection.fieldA", "ignoreFieldB.fieldValue");
```

This approach is very similar to web field binding except that collections are not indexed. This means that while you can refer to a field of an object within a collection, it will always apply to all objects in the collection, not any single one. For example you cannot specify '`ignoreCollection[0].fieldName`' to ignore only the '`fieldName`' property of the first element in the ignoreCollection. I can't think of a situation where this would be that useful. Having said that, adding this capability would be reasonably straight forward and may come later.


## Wildcard Exclusions ##

There are times when you want to convert an object that is a property of another but not actually convert any of it's properties. Say for example, you want to look up a staff member's manager from the database using the `createDestinationObject(...)` method (described below) and populate the manager field in the staff member but you don't want to actually map any of the `ManagerDto` fields onto the Manager object from the database. In this case you can use a wildcard in your ignorePath.

```
StaffMember staffMember = objectAssembler.assemble(staffMemberDto, StaffMember.class, "manager.*");
```

The effect of this is that the constructed destination object is simply returned from the converter and no conversion is performed on any of the fields. This also means that any custom conversion logic will be bypassed.

## Mapping fields with different names ##

Sometimes your source and destination objects don't have the same names. In this case, it's possible to define pairs of 'source > destination' names for mapping. This is done at the converter level by overriding the `customConverterFieldMappings()` method. This method should return a Set of `ConverterFieldMapping`s that describe the source and destination field names.

In practice, you may choose to simply map these fields using java using the `convert` method discussed below as this is more likely to pick up future refactoring. One case that this approach might be useful in is where the destination object does not have setter methods for the fields. In this case the converter will fall back to property access.


## Configuration Validation ##

It's easy to mistype a property name so you'll want some checking to make sure your property names actually exist. Due to the way converters are registered, this can't be done at startup time yet.  It relies on all converters being registered before it can correctly verify the existence of converters for combinations of source / destination types. It can be done on first execution though so it's easy to pick this up as part of your test suite. Any fields marked to ignore or fields mapped that don't exist will result in an exception with a clear indication as to the source of the problem.

## Constructing your own Destination Object ##

The default behaviour is for the converter to attempt to construct a destination object based on it's destination type. This requires your destination object to have a default no-arg constructor. If you want to be able to construct your own object you can do this by overriding the `createDestinationObject(Source source)` method. Because the `source` object is passed into this method it's entirely possible to manually do some conversion in here. This however is not the intended place and it's possible that your values will be overridden during the actual conversion which happens next so it's not advised. There is a specific method for this which i'll come to. This is however a good place to do things like retrieve an object from the database based on the id of the source object (the most common case), or use a factory to construct the destination object.

Example of a Converter with the `createDestinationObject` method being used.


```
@Component
public class UserDtoToUserConverter extends AbstractObjectConverter<UserDto, User> {

  private UserDao userDao;

  @Autowired
  public UserDtoToUserConverter(UserDao userDao) {
    super();
    this.userDao = userDao;
  }

  @Override
  public User createDestinationObject(UserDto userDto) {
    if (userDto.getId() == null) {
       return new User();
    } else {
      return userDao.findById(userDto.getId());
    }
  }

}
```

## Custom Conversion Logic ##

There are cases where you need to perform some custom conversion logic in java because it's not simple enough to just map one field to another. In this case you can override the `convert(Source source, Destination destination)` method. This is called after all auto conversion has been performed and enables any custom conversion to be done. If you wanted to perform a full custom conversion and prevent automapping for a converter you can simply override `disableAutoMapping()` to return true and write your custom logic here.





## Some things to consider ##

This is solution won't make sense in all situations. Some people will take issue with the idea of passing strings of property fields to ignore / exclude into the assembler due to the lack of type safety. This felt a little 'dirty' to me at the start but in practice after using this now on multiple applications it's turned out to be a non-issue and the benefits have outweighed the potential fragility significantly.

I've found that a sweet spot for this approach to object assembly /conversion is in typical data-driven web apps using Spring and Hibernate. The configuration is simple and even if you don't use any automatic conversion there is some pay off in code organisation from the converter registry.  As rapid development frameworks get smarter, we need to be wary of over-engineering and I think this approach helps to keep it simple while enabling a separation of our persistent back-end objects and our view layer domain / remote objects.