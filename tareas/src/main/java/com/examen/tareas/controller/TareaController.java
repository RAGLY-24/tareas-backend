package com.examen.tareas.controller;
import com.examen.tareas.entity.Tarea;
import com.examen.tareas.entity.Usuario;
import com.examen.tareas.repository.TareaRepository;
import com.examen.tareas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tareas")
public class TareaController {

    @Autowired private TareaRepository tareaRepo;
    @Autowired private UsuarioRepository usuarioRepo;

    // Método helper para obtener el usuario que está haciendo la petición usando su Token
    private Usuario getUsuarioAutenticado() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepo.findByCorreo(correo).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // GET: Obtiene SOLO las tareas del usuario logueado
    @GetMapping
    public ResponseEntity<List<Tarea>> getTareas() {
        Usuario usuario = getUsuarioAutenticado();
        return ResponseEntity.ok(tareaRepo.findByUsuarioId(usuario.getId()));
    }

    // POST: Crea una nueva tarea asociada al usuario logueado
    @PostMapping
    public ResponseEntity<Tarea> createTarea(@RequestBody Tarea tarea) {
        tarea.setUsuario(getUsuarioAutenticado());
        if(tarea.getEstado() == null) tarea.setEstado("PENDIENTE");
        return ResponseEntity.status(HttpStatus.CREATED).body(tareaRepo.save(tarea));
    }

    // PUT: Actualiza una tarea asegurándose de que le pertenezca al usuario
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTarea(@PathVariable Long id, @RequestBody Tarea tareaActualizada) {
        Tarea tarea = tareaRepo.findById(id).orElse(null);
        if (tarea == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tarea no encontrada");

        // Validación de seguridad (403 Forbidden si intenta editar la tarea de otro)
        if (!tarea.getUsuario().getId().equals(getUsuarioAutenticado().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para editar esta tarea");
        }

        tarea.setTitulo(tareaActualizada.getTitulo());
        tarea.setDescripcion(tareaActualizada.getDescripcion());
        tarea.setEstado(tareaActualizada.getEstado());

        return ResponseEntity.ok(tareaRepo.save(tarea));
    }

    // DELETE: Elimina una tarea asegurándose de que le pertenezca al usuario
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTarea(@PathVariable Long id) {
        Tarea tarea = tareaRepo.findById(id).orElse(null);
        if (tarea == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tarea no encontrada");

        // Validación de seguridad
        if (!tarea.getUsuario().getId().equals(getUsuarioAutenticado().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para eliminar esta tarea");
        }

        tareaRepo.delete(tarea);
        return ResponseEntity.ok(Map.of("mensaje", "Tarea eliminada"));
    }
}