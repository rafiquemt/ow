package net.vtst.eclipse.easyxtext.validation;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator.State;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator.StateAccess;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

public class ConfigurableValidationMessageAcceptor implements ValidationMessageAcceptor {
  
  private DeclarativeValidatorInspector inspector;
  private ValidationMessageAcceptor delegate;
  StateAccess stateAccess;

  public ConfigurableValidationMessageAcceptor(DeclarativeValidatorInspector inspector, ValidationMessageAcceptor delegate) {
    this.inspector = inspector;
    this.delegate = delegate;
  }
    
  // **************************************************************************
  // Configuration cache
  
  public class ProjectConfiguration {
    public Set<Method> disabledCheckMethods = new HashSet<Method>();
    
    public ProjectConfiguration(IProject project) {
      try {
        for (DeclarativeValidatorInspector.Group group : inspector.getGroups()) {
          if (!inspector.getEnabled(project, group)) {
            disabledCheckMethods.addAll(group.methods);
          }
        }
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }
  }
  
  public class Cache {
    WeakHashMap<IProject, ProjectConfiguration> configurations = new WeakHashMap<IProject, ProjectConfiguration>();
    private Resource lastResource;
    private ProjectConfiguration lastConfiguration;
    
    private IProject getProject(Resource resource) {
      URI uri = resource.getURI();
      IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(uri.toPlatformString(true)));
      if (file == null) return null;
      return file.getProject();
    }
        
    private ProjectConfiguration get(IProject project) {
      if (project == null) return null;
      ProjectConfiguration configuration = configurations.get(project);
      if (configuration == null) {
        configuration = new ProjectConfiguration(project);
        configurations.put(project, configuration);
      }
      return configuration;
    }

    public ProjectConfiguration get(Resource resource) {
      if (!resource.equals(lastResource)) {
        lastResource = resource;
        lastConfiguration = get(getProject(resource));
      }
      return lastConfiguration;
    }
    
    public void reset(IProject project) {
      configurations.remove(project);
    }
  }
  
  private Cache cache = new Cache();

  public void resetCache(IProject project) {
    cache.reset(project);
  }

  // **************************************************************************
  // implements ValidationMessageAcceptor

  private boolean isDisabled() {
    State state = stateAccess.getState();
    if (state.currentObject == null) return false;
    ProjectConfiguration configuration = cache.get(state.currentObject.eResource());
    if (configuration == null) return false;
    return configuration.disabledCheckMethods.contains(state.currentMethod);
  }

  @Override
  public void acceptError(String message, EObject object,
      EStructuralFeature feature, int index, String code, String... issueData) {
    if (isDisabled()) return;
    delegate.acceptError(message, object, feature, index, code, issueData);
  }

  @Override
  public void acceptError(String message, EObject object, int offset,
      int length, String code, String... issueData) {
    if (isDisabled()) return;
    delegate.acceptError(message, object, offset, length, code, issueData);
  }

  @Override
  public void acceptWarning(String message, EObject object,
      EStructuralFeature feature, int index, String code, String... issueData) {
    if (isDisabled()) return;
    delegate.acceptWarning(message, object, feature, index, code, issueData);
  }

  @Override
  public void acceptWarning(String message, EObject object, int offset,
      int length, String code, String... issueData) {
    if (isDisabled()) return;
    delegate.acceptWarning(message, object, offset, length, code, issueData);
  }

  @Override
  public void acceptInfo(String message, EObject object,
      EStructuralFeature feature, int index, String code, String... issueData) {
    if (isDisabled()) return;
    delegate.acceptInfo(message, object, feature, index, code, issueData);
  }

  @Override
  public void acceptInfo(String message, EObject object, int offset,
      int length, String code, String... issueData) {
    if (isDisabled()) return;
    delegate.acceptInfo(message, object, offset, length, code, issueData);
  }

}
