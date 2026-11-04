const questions = [
  ['¿Cómo inicio sesión?', 'Selecciona tu rol, escribe el correo y la contraseña registrados y pulsa Continuar. La sesión se conservará hasta que elijas Cerrar sesión.'],
  ['¿Cómo solicito un inmueble?', 'Como inquilino, abre una propiedad disponible, revisa sus datos y envía la solicitud. El propietario podrá aprobarla o rechazarla.'],
  ['¿Dónde consulto mis contratos?', 'Abre Contratos desde la navegación. Allí verás la vigencia, el valor mensual, el estado y el historial de pagos asociado.'],
  ['¿Cómo reporto una falla?', 'En Fallas, pulsa Reportar una falla. Solo aparecen inmuebles para los que tienes un contrato activo.'],
  ['¿Cómo cambia el estado de una falla?', 'El propietario responsable puede pasarla de pendiente a en proceso y luego a resuelta. Cada cambio genera una notificación.'],
  ['¿Cómo envío un mensaje?', 'Abre Conversaciones y selecciona el inmueble y la persona. El chat solo está disponible entre participantes con relación contractual.'],
  ['¿Cómo cierro mi sesión?', 'Usa el botón Cerrar sesión. Pulsar Atrás o cerrar la aplicación no elimina la sesión almacenada.'],
  ['¿Qué hace el panel administrador?', 'Muestra un resumen de solo lectura de usuarios, propiedades, solicitudes, contratos y fallas. Requiere una cuenta con rol administrador.']
];

const container = document.querySelector('#faq');
const search = document.querySelector('#search');
const empty = document.querySelector('#empty');

function render(filter = '') {
  const term = filter.trim().toLocaleLowerCase('es');
  const visible = questions.filter(([question, answer]) =>
    `${question} ${answer}`.toLocaleLowerCase('es').includes(term)
  );
  container.replaceChildren(...visible.map(([question, answer]) => {
    const article = document.createElement('article');
    article.className = 'item';
    const button = document.createElement('button');
    button.className = 'question';
    button.type = 'button';
    button.textContent = question;
    button.setAttribute('aria-expanded', 'false');
    const paragraph = document.createElement('p');
    paragraph.className = 'answer';
    paragraph.textContent = answer;
    paragraph.hidden = true;
    button.addEventListener('click', () => {
      const open = article.classList.toggle('open');
      button.setAttribute('aria-expanded', String(open));
      paragraph.hidden = !open;
    });
    article.append(button, paragraph);
    return article;
  }));
  empty.hidden = visible.length > 0;
}

search.addEventListener('input', event => render(event.target.value));
render();
