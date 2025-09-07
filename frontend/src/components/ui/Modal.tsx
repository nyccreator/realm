import React from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {Dialog} from '@headlessui/react';
import {X} from 'lucide-react';
import {cn} from '../../utils/cn';
import {Button} from './Button';

export interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  description?: string;
  children: React.ReactNode;
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full';
  className?: string;
  closeOnOverlayClick?: boolean;
  showCloseButton?: boolean;
}

const sizeClasses = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl',
  full: 'max-w-[95vw] max-h-[95vh]',
};

const Modal: React.FC<ModalProps> = ({
  open,
  onClose,
  title,
  description,
  children,
  size = 'md',
  className,
  closeOnOverlayClick = true,
  showCloseButton = true,
}) => {
  return (
    <AnimatePresence>
      {open && (
        <Dialog
          as={motion.div}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          open={open}
          onClose={closeOnOverlayClick ? onClose : () => {}}
          className="fixed inset-0 z-50 overflow-y-auto"
        >
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm"
            onClick={closeOnOverlayClick ? onClose : undefined}
          />

          {/* Modal container */}
          <div className="flex min-h-full items-center justify-center p-4">
            <Dialog.Panel
              as={motion.div}
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              transition={{ type: 'spring', stiffness: 400, damping: 25 } as any}
              className={cn(
                'relative w-full bg-white rounded-2xl shadow-xl border border-gray-200',
                'transform overflow-hidden text-left align-middle transition-all',
                sizeClasses[size],
                className
              )}
            >
              {/* Header */}
              {(title || description || showCloseButton) && (
                <div className="flex items-start justify-between p-6 pb-4">
                  <div className="flex-1">
                    {title && (
                      <Dialog.Title
                        as="h3"
                        className="text-lg font-semibold leading-6 text-gray-900"
                      >
                        {title}
                      </Dialog.Title>
                    )}
                    {description && (
                      <Dialog.Description className="mt-1 text-sm text-gray-600">
                        {description}
                      </Dialog.Description>
                    )}
                  </div>
                  {showCloseButton && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={onClose}
                      className="ml-4 -mr-2 -mt-2 h-8 w-8 p-0 text-gray-400 hover:text-gray-600"
                    >
                      <X className="h-4 w-4" />
                      <span className="sr-only">Close</span>
                    </Button>
                  )}
                </div>
              )}

              {/* Content */}
              <div className="px-6 pb-6">
                {children}
              </div>
            </Dialog.Panel>
          </div>
        </Dialog>
      )}
    </AnimatePresence>
  );
};

const ModalFooter: React.FC<{
  children: React.ReactNode;
  className?: string;
}> = ({ children, className }) => (
  <div className={cn('flex justify-end space-x-3 pt-4 border-t border-gray-100', className)}>
    {children}
  </div>
);

export { Modal, ModalFooter };