(function () {
  function closeAll(modules) {
    modules.forEach(function (module) {
      module.classList.remove('is-open');
    });
  }

  window.addEventListener('DOMContentLoaded', function () {
    var modules = Array.prototype.slice.call(
      document.querySelectorAll('.dashboard-nav-search')
    );

    if (!modules.length) {
      return;
    }

    modules.forEach(function (module) {
      var toggle = module.querySelector('.dashboard-nav-search-toggle');
      var input = module.querySelector('input[type="search"]');

      if (!toggle) {
        return;
      }

      toggle.addEventListener('click', function (event) {
        event.preventDefault();
        event.stopPropagation();

        var isOpen = module.classList.contains('is-open');
        closeAll(modules);

        if (!isOpen) {
          module.classList.add('is-open');
          if (input) {
            input.focus();
          }
        }
      });

      module.addEventListener('click', function (event) {
        event.stopPropagation();
      });

      if (input) {
        input.addEventListener('keydown', function (event) {
          if (event.key === 'Escape') {
            module.classList.remove('is-open');
            toggle.focus();
          }
        });
      }
    });

    document.addEventListener('click', function () {
      closeAll(modules);
    });

    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') {
        closeAll(modules);
      }
    });
  });
})();
