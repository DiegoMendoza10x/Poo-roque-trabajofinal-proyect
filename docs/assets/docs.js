// Resalta la entrada del índice según la sección visible.
(function () {
  const enlaces = Array.from(document.querySelectorAll('.indice a'));
  if (!enlaces.length || !('IntersectionObserver' in window)) return;
  const observer = new IntersectionObserver(entradas => {
    entradas.forEach(e => {
      if (e.isIntersecting) {
        enlaces.forEach(a => a.classList.toggle(
          'activo', a.getAttribute('href') === '#' + e.target.id));
      }
    });
  }, { rootMargin: '-15% 0px -75% 0px' });
  enlaces.forEach(a => {
    const seccion = document.querySelector(a.getAttribute('href'));
    if (seccion) observer.observe(seccion);
  });
})();
