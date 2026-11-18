import { useState, type ComponentProps } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import Campo from './Campo'
import Boton from './Boton'

type CampoContrasenaProps = Omit<ComponentProps<typeof Campo>, 'type'>

export default function CampoContrasena(props: CampoContrasenaProps) {
  const [visible, setVisible] = useState(false)

  return (
    <div>
      <Campo {...props} type={visible ? 'text' : 'password'} />
      <Boton
        variante="fantasma"
        aria-pressed={visible}
        onClick={() => setVisible(actual => !actual)}
        className="mt-1"
      >
        {visible ? (
          <EyeOff aria-hidden="true" className="size-4" />
        ) : (
          <Eye aria-hidden="true" className="size-4" />
        )}
        {visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
      </Boton>
    </div>
  )
}
