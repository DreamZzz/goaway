export const navigateToRoute = (navigation, routeName, params) => {
  let current = navigation;

  while (current) {
    try {
      current.navigate(routeName, params);
      return true;
    } catch {
      current = typeof current.getParent === 'function' ? current.getParent() : null;
    }
  }

  return false;
};

export const resetToRoute = (navigation, routeName, params) => {
  let current = navigation;

  while (current) {
    if (typeof current.reset === 'function') {
      current.reset({
        index: 0,
        routes: [{ name: routeName, params }],
      });
      return true;
    }
    current = typeof current.getParent === 'function' ? current.getParent() : null;
  }

  return navigateToRoute(navigation, routeName, params);
};
